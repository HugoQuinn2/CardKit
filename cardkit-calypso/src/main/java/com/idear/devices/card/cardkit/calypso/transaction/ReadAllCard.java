package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.ReadCardData;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SimpleReadCard;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;

/**
 * Represents a transaction that performs a complete read operation on a Calypso card.
 * <p>
 * This transaction first verifies that a card is present in the reader by performing a
 * {@link SimpleReadCard} operation. If the card is detected, it proceeds to read all
 * the available card data using {@link ReadCardData} with a specific {@link WriteAccessLevel}.
 * </p>
 *
 * <p>
 * The result contains the fully populated {@link CalypsoCardCDMX} instance if the
 * operation is successful. If any step fails, a {@link ReaderException} or
 * {@link CardException} is thrown, depending on the cause.
 * </p>
 *
 * @see CalypsoCardCDMX
 * @see ReaderPCSC
 * @see ReadCardData
 * @see SimpleReadCard
 * @see Transaction
 * @see TransactionResult
 * @see TransactionStatus
 * @see WriteAccessLevel
 *
 * @author Victor Hugo Gaspar Quinn
 */
public class ReadAllCard extends Transaction<CalypsoCardCDMX, ReaderPCSC> {

    /**
     * Creates a new {@code ReadAllCard} transaction with the default name "read all card".
     */
    public ReadAllCard() {
        super("read all card");
    }

    /**
     * Executes the full card reading process.
     * <p>
     * The method performs the following steps:
     * </p>
     * <ol>
     *     <li>Executes a {@link SimpleReadCard} transaction to detect if a card is present.</li>
     *     <li>If no card is found, a {@link ReaderException} is thrown.</li>
     *     <li>If a card is detected, performs a {@link ReadCardData} transaction with
     *     {@link WriteAccessLevel#DEBIT} to retrieve all card data.</li>
     *     <li>If reading fails, a {@link CardException} is thrown.</li>
     *     <li>If successful, returns a {@link TransactionResult} with {@link TransactionStatus#OK}
     *     containing the full card data.</li>
     * </ol>
     *
     * @param reader the {@link ReaderPCSC} instance used to communicate with the Calypso card
     * @return a {@link TransactionResult} containing the fully read {@link CalypsoCardCDMX} instance
     * @throws ReaderException if no card is detected in the reader
     * @throws CardException if there is an error while reading the card data
     */
    @Override
    public TransactionResult<CalypsoCardCDMX> execute(ReaderPCSC reader) {

        TransactionResult<CalypsoCardCDMX> simpleRead = reader.execute(new SimpleReadCard());
        if (!simpleRead.isOk())
            throw new ReaderException("No card on reader");

        CalypsoCardCDMX calypsoCardCDMX = simpleRead.getData();

        TransactionResult<CalypsoCardCDMX> dataCard = reader.execute(new ReadCardData(WriteAccessLevel.DEBIT, calypsoCardCDMX));
        calypsoCardCDMX = dataCard.getData();

        if (!dataCard.isOk())
            throw new CardException("Error reading card data: " + dataCard.getMessage());

        return TransactionResult
                .<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCardCDMX)
                .message("All card data '" + calypsoCardCDMX.getSerial() + "' was read")
                .build();
    }
}
