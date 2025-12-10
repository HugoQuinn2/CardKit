package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Getter;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Transaction to reload a card with a specified amount and renew its contract if necessary.
 * <p>
 * This transaction performs the following operations:
 * <ol>
 *   <li>Verifies that a card is present and matches the expected card.</li>
 *   <li>Checks the contract status and validates maximum balance constraints.</li>
 *   <li>Renews the contract if applicable.</li>
 *   <li>Creates a reload event and performs the reload operation on the card.</li>
 * </ol>
 *
 * <p>Progress updates are sent via {@link #reportProgress(int, String)}, which can be observed externally.
 */
@Getter
public class ReloadCard extends Transaction<Boolean, KeypleReader> {

    public static final String NAME = "RELOAD_CARD";

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Provider provider;
    private final Contract contract;
    private final int passenger;
    private final LocationCode locationId;
    private final int amount;

    /**
     * Constructs a reload and renew transaction for a specific card.
     *
     * @param calypsoCardCDMX The card to reload.
     * @param amount          The amount to add to the card balance.
     * @param contract        The contract to renew if applicable.
     * @param locationId      The location ID performing the operation.
     */
    public ReloadCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Provider provider,
            LocationCode locationId,
            int amount,
            Contract contract,
            int passenger) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.provider = provider;
        this.amount = amount;
        this.contract = contract;
        this.passenger = passenger;
        this.locationId = locationId;
    }

    /**
     * Constructs a reload and renew transaction for a specific card.
     *
     * @param calypsoCardCDMX The card to reload.
     * @param amount          The amount to add to the card balance.
     * @param locationId      The location ID performing the operation.
     */
    public ReloadCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Provider provider,
            LocationCode locationId,
            int amount) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.provider = provider;
        this.amount = amount;
        this.passenger = 0;
        this.locationId = locationId;

        this.contract = calypsoCardCDMX.getContracts()
                .findFirst(c -> c.getStatus().isAccepted())
                .orElseThrow(() -> new CardException(
                        "card '%s' without valid contract", calypsoCardCDMX.getSerial()));
    }

    /**
     * Executes the reload and renew operation.
     *
     * @param reader The reader interface used to communicate with the card.
     * @return TransactionResult containing success/failure and status message.
     */
    @Override
    public TransactionResult<Boolean> execute(KeypleReader reader) {

        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("card invalidated");


        // Step 4: Perform reload operation
        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .prepareSvReload(
                        amount,
                        CompactDate.now().toBytes(),
                        CompactTime.now().toBytes(),
                        ByteUtils.extractBytes(0, 2))
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.execute(
                new SaveEvent(
                        calypsoCardCDMX,
                        TransactionType.RELOAD.getValue(),
                        calypsoCardCDMX.getEnvironment().getNetwork().getValue(),
                        provider.getValue(),
                        locationId,
                        contract,
                        passenger,
                        calypsoCardCDMX.getEvents().getNextTransactionNumber(),
                        amount
                )
        );

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message(String.format("Final card balance for '%s': %s", calypsoCardCDMX.getSerial(), calypsoCardCDMX.getBalance() + amount))
                .build();
    }
}
