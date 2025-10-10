package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SimpleReadCard;
import com.idear.devices.card.cardkit.core.datamodel.calypso.ContractStatus;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
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
public class ReloadAndRenewCard extends Transaction<Boolean, ReaderPCSC> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int locationId;
    private final int amount;

    private int provider = 0;
    private int maxBalance = 500_000;
    private int contractDaysOffset = 15;

    /**
     * Constructs a reload and renew transaction for a specific card.
     *
     * @param calypsoCardCDMX The card to reload.
     * @param amount          The amount to add to the card balance.
     * @param contract        The contract to renew if applicable.
     * @param locationId      The location ID performing the operation.
     */
    public ReloadAndRenewCard(CalypsoCardCDMX calypsoCardCDMX, int amount, Contract contract, int locationId) {
        super("reload and renew card");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.amount = amount;
        this.contract = contract;
        this.locationId = locationId;
    }

    /**
     * Executes the reload and renew operation.
     *
     * @param reader The reader interface used to communicate with the card.
     * @return TransactionResult containing success/failure and status message.
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        reportProgress(0, "Starting reload transaction");

        // Step 1: Read card
        TransactionResult<CalypsoCardCDMX> simpleRead = reader.execute(new SimpleReadCard());
        if (!simpleRead.isOk()) throw new ReaderException("No card on reader");

        if (!simpleRead.getData().getSerial().equals(calypsoCardCDMX.getSerial()))
            throw new CardException("Invalid card: expected %s, got %s",
                    calypsoCardCDMX.getSerial(),
                    simpleRead.getData().getSerial());

        reportProgress(10, "Card verified");

        // Step 2: Check contract validity
        if (!contract.getStatus().equals(ContractStatus.CONTRACT_PARTLY_USED))
            throw new CardException("No valid contract on card");

        if (calypsoCardCDMX.getBalance() + amount > maxBalance)
            throw new CardException("Final balance exceeds maximum (%s): %s", maxBalance, calypsoCardCDMX.getBalance() + amount);

        reportProgress(20, "Contract validated and balance checked");

        // Step 3: Renew contract
        if (contract.isExpired(contractDaysOffset))
            reader.execute(new RenewedContract(calypsoCardCDMX, locationId, contract, contractDaysOffset));
        reportProgress(40, "Contract renewed");

        // Step 4: Create reload event
        Event event = Event.builEvent(
                TransactionType.RELOAD,
                reader.getCalypsoSam(),
                calypsoCardCDMX.getEvents().getLast().getTransactionNumber() + 1,
                locationId,
                amount
        );

        if (provider > 0) event.setProvider(provider);

        reportProgress(50, "Reload event prepared");

        // Step 5: Perform reload operation
        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .processCommands(ChannelControl.KEEP_OPEN);

        reportProgress(70, "Secure session started");

        reader.getCardTransactionManager()
                .prepareSvReload(
                        amount,
                        CompactDate.toBytes(CompactDate.now().getCode()),
                        CompactTime.toBytes(CompactTime.now().getCode()),
                        ByteUtils.extractBytes(0, 2)
                )
                .prepareAppendRecord(event.getFileId(), event.unparse())
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER);

        reportProgress(100, "Reload successful");

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message(String.format("Final card balance for '%s': %s", calypsoCardCDMX.getSerial(), calypsoCardCDMX.getBalance() + amount))
                .build();
    }

    /**
     * Sets the maximum allowed balance on the card.
     *
     * @param maxBalance Maximum balance
     * @return This transaction for chaining
     */
    public ReloadAndRenewCard maxBalance(int maxBalance) {
        this.maxBalance = maxBalance;
        return this;
    }

    /**
     * Sets the number of days offset to consider for contract renewal.
     *
     * @param contractDaysOffset Days offset
     * @return This transaction for chaining
     */
    public ReloadAndRenewCard contractDaysOffset(int contractDaysOffset) {
        this.contractDaysOffset = contractDaysOffset;
        return this;
    }

    /**
     * Sets the provider identifier for the reload event.
     *
     * @param provider Provider code
     * @return This transaction for chaining
     */
    public ReloadAndRenewCard provider(int provider) {
        this.provider = provider;
        return this;
    }
}
