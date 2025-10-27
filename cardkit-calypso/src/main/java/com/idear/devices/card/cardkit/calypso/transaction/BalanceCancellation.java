package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.NetworkCode;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Represents a transaction that cancels the balance of a Calypso card.
 * <p>
 * This transaction performs a read operation to ensure a card is present,
 * then executes a reload/renew process with a negative amount equivalent
 * to the current balance, effectively setting the card's balance to zero.
 * </p>
 *
 * <p>
 * The cancellation is performed through a {@link ReloadCard} transaction.
 * If the process is successful, the result will contain {@code true} and a status of {@link TransactionStatus#OK}.
 * Otherwise, it will contain {@code false} and a status of {@link TransactionStatus#ERROR}.
 * </p>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 * @see CalypsoCardCDMX
 * @see ReloadCard
 * @see Transaction
 */
public class BalanceCancellation extends Transaction<Boolean, ReaderPCSC> {

    public static final String NAME = "BALANCE_CANCELLATION";

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final TransactionType transactionType;
    private final Provider provider;
    private final LocationCode locationId;
    private final Contract contract;

    /**
     * Constructs a new {@code BalanceCancellation} transaction.
     *
     * @param calypsoCardCDMX the Calypso card instance whose balance will be canceled
     * @param locationId      the ID of the location where the transaction is performed
     * @param contract        the contract associated with the transaction
     * @param transactionType the type of transaction being executed
     */
    public BalanceCancellation(
            CalypsoCardCDMX calypsoCardCDMX,
            TransactionType transactionType,
            Provider provider,
            LocationCode locationId,
            Contract contract) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.provider = provider;
        this.locationId = locationId;
        this.contract = contract;
        this.transactionType = transactionType;
    }

    /**
     * Constructs a new {@code BalanceCancellation} transaction with the first {@link Contract} accepted founded.
     *
     * @param calypsoCardCDMX the Calypso card instance whose balance will be canceled
     * @param locationId      the ID of the location where the transaction is performed
     * @param transactionType the type of transaction being executed
     */
    public BalanceCancellation(
            CalypsoCardCDMX calypsoCardCDMX,
            TransactionType transactionType,
            Provider provider,
            LocationCode locationId) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.locationId = locationId;
        this.transactionType = transactionType;

        this.contract = calypsoCardCDMX.getContracts()
                .findFirst(c -> c.getStatus().isAccepted())
                .orElseThrow(() -> new CardException(
                        "card '%s' without valid contract", calypsoCardCDMX.getSerial()));
        this.provider = provider;
    }

    /**
     * Executes the balance cancellation transaction.
     * <p>
     * The method first checks if a card is present in the reader.
     * If not, a {@link CardException} is thrown.
     * It then creates a {@link ReloadCard} transaction with a negative amount
     * equal to the current card balance and a contract offset of 1800 days (approximately 60 months).
     * </p>
     *
     * @param reader the reader interface used to communicate with the card
     * @return a {@link TransactionResult} containing the success status and related message
     * @throws CardException if no card is detected in the reader
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("card invalidated");

        int negativeAmount = calypsoCardCDMX.getBalance() * (-1);

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .prepareSvReload(
                        negativeAmount,
                        CompactDate.now().toBytes(),
                        CompactTime.now().toBytes(),
                        ByteUtils.extractBytes(0, 2))
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.execute(new SaveEvent(
                calypsoCardCDMX,
                transactionType,
                calypsoCardCDMX.getEnvironment().getNetwork().decodeOrElse(NetworkCode.RFU),
                provider,
                locationId,
                contract,
                0,
                negativeAmount,
                calypsoCardCDMX.getEvents().getNextTransactionNumber())
        );

        return TransactionResult.
                <Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message("balance card '" + calypsoCardCDMX.getSerial() + "' canceled")
                .build();

    }
}
