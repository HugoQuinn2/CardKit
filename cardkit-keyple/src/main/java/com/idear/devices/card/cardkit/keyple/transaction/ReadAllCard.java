package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.ReadCardData;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.ReadCardFile;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.ReadCardFilePartially;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.SimpleReadCard;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.CalypsoProduct;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.*;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.SvDebitLogRecord;
import org.eclipse.keypop.calypso.card.card.SvLoadLogRecord;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

import java.util.SortedMap;

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
 * @see KeypleReader
 * @see ReadCardData
 * @see SimpleReadCard
 * @see Transaction
 * @see TransactionResult
 * @see TransactionStatus
 * @see WriteAccessLevel
 *
 * @author Victor Hugo Gaspar Quinn
 */
@Slf4j
public class ReadAllCard extends Transaction<CalypsoCardCDMX, KeypleReader> {

    public static final String NAME = "READ_ALL_CARD";

    private CalypsoCardCDMX calypsoCardCDMX;
    private TransactionResult<byte[]> readFile;
    private TransactionResult<SortedMap<Integer, byte[]>> readFiles;

    /**
     * Creates a new {@code ReadAllCard} transaction with the default name "read all card".
     */
    public ReadAllCard() {
        super(NAME);
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
     * @param reader the {@link KeypleReader} instance used to communicate with the Calypso card
     * @return a {@link TransactionResult} containing the fully read {@link CalypsoCardCDMX} instance
     * @throws ReaderException if no card is detected in the reader
     * @throws CardException if there is an error while reading the card data
     */
    @Override
    public TransactionResult<CalypsoCardCDMX> execute(KeypleReader reader) {

        // Initialize the card model to store the read data
        calypsoCardCDMX = new CalypsoCardCDMX();

        // Ensure the reader session is up-to-date
        CalypsoCard calypsoCard = reader.getCalypsoCard();

        // Check if the Dedicated File (DF) is invalid
        calypsoCardCDMX.setEnabled(!calypsoCard.isDfInvalidated());

        // Extract and set the card serial number
        calypsoCardCDMX.setSerial(HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));

        if (reader.getCardTransactionManager() != null)
            reader.getCardTransactionManager().processCommands(ChannelControl.CLOSE_AFTER);

        readLogFiles(reader, calypsoCard);
        readEnvironmentFile(reader);
        readEventFiles(reader);
        readContractFiles(reader);

        calypsoCardCDMX.setCalypsoProduct(CalypsoProduct.parseByCalypsoCard(calypsoCard));

//        cardTransactionManager.processCommands(ChannelControl.CLOSE_AFTER);
        return TransactionResult
                .<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCardCDMX)
                .message("All card data '" + calypsoCardCDMX.getSerial() + "' was read")
                .build();
    }

    private void readLogFiles(
            KeypleReader reader,
            CalypsoCard calypsoCard) {
        try {
            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);

            SvDebitLogRecord debitLogRecord = reader.getCalypsoCard().getSvDebitLogLastRecord();
            if (debitLogRecord != null) {
                calypsoCardCDMX.setDebitLog(new DebitLog().parse(debitLogRecord));
            } else {
                log.debug("failed to read debit log record");
            }

            SvLoadLogRecord loadLogRecord = reader.getCalypsoCard().getSvLoadLogRecord();
            if (loadLogRecord != null) {
                calypsoCardCDMX.setLoadLog(new LoadLog().parse(loadLogRecord));
            } else {
                log.debug("failed to read load log record");
            }

            calypsoCardCDMX.setBalance(calypsoCard.getSvBalance());
        } catch (Exception e) {
            log.debug("Error reading logs files");
        }
    }

    private void readEnvironmentFile(KeypleReader reader) {
        readFile = reader.execute(
                new ReadCardFile(WriteAccessLevel.DEBIT, Calypso.ENVIRONMENT_FILE, 1)
        );

        if (readFile.isOk()) {
            calypsoCardCDMX.setEnvironment(new Environment().parse(readFile.getData()));
        } else {
            log.debug("failed to read environment file: {}", readFile.getMessage());
        }
    }

    private void readEventFiles(KeypleReader reader) {
        readFiles = reader.execute(
                new ReadCardFilePartially(Calypso.EVENT_FILE, (byte) 1, (byte) 3, 0, 29)
        );

        if (!readFiles.isOk()) {
            log.debug("Failed to read events file: {}", readFiles.getMessage());
            return;
        }

        Events events = new Events();
        SortedMap<Integer, byte[]> partialEvents = readFiles.getData();
        for (var entry : partialEvents.entrySet()) {
            events.add(new Event(entry.getKey()).parse(entry.getValue()));
        }
        calypsoCardCDMX.setEvents(events);
    }

    private void readContractFiles(KeypleReader reader) {
        readFiles = reader.execute(
                new ReadCardFilePartially(Calypso.CONTRACT_FILE, (byte) 1, (byte) 8, 0, 10)
        );

        if (!readFiles.isOk()) {
            log.debug("Failed to read contracts file: {}", readFiles.getMessage());
            return;
        }

        Contracts contracts = new Contracts();
        SortedMap<Integer, byte[]> partialContracts = readFiles.getData();
        for (var entry : partialContracts.entrySet()) {
            contracts.add(
                    new Contract(entry.getKey())
                            .parse(entry.getValue())
            );
        }
        calypsoCardCDMX.setContracts(contracts);
    }

}
