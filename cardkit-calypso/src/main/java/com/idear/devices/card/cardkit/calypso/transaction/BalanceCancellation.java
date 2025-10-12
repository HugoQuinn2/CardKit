package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SimpleReadCard;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;

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
 * @see CalypsoCardCDMX
 * @see ReloadCard
 * @see Transaction
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
public class BalanceCancellation extends Transaction<Boolean, ReaderPCSC> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final int locationId;
    private final Contract contract;
    private final TransactionType transactionType;

    /**
     * Constructs a new {@code BalanceCancellation} transaction.
     *
     * @param calypsoCardCDMX the Calypso card instance whose balance will be canceled
     * @param locationId the ID of the location where the transaction is performed
     * @param contract the contract associated with the transaction
     * @param transactionType the type of transaction being executed
     */
    public BalanceCancellation(
            CalypsoCardCDMX calypsoCardCDMX,
            int locationId,
            Contract contract,
            TransactionType transactionType) {
        super("balance cancellation");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.locationId = locationId;
        this.contract = contract;
        this.transactionType = transactionType;
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
        if (!reader.execute(new SimpleReadCard()).isOk())
            throw new CardException("no card on reader");

        int negativeAmount = calypsoCardCDMX.getBalance() * -1;
        TransactionResult<Boolean> canceledBalance = reader.execute(
                new ReloadCard(
                        calypsoCardCDMX,
                        negativeAmount,
                        contract,
                        locationId)
                        .transactionType(transactionType)
                        .contractDaysOffset(1800));

        if (canceledBalance.isOk())
            return TransactionResult.
                    <Boolean>builder()
                    .transactionStatus(TransactionStatus.OK)
                    .data(true)
                    .message(transactionType + ": card '" + calypsoCardCDMX.getSerial() + "' canceled")
                    .build();


        return TransactionResult.
                <Boolean>builder()
                .transactionStatus(TransactionStatus.ERROR)
                .data(false)
                .message("Error canceling card balance: " + canceledBalance.getMessage())
                .build();
    }
}
