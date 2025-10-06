package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.SvDebitLogRecord;
import org.eclipse.keypop.calypso.card.card.SvLoadLogRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Reads all Calypso card data and maps it to {@link CalypsoCardCDMX}.
 * <p>
 * This transaction performs:
 * <ul>
 *     <li>A simple read to retrieve basic information.</li>
 *     <li>Reading of environment, events, contracts, and log files.</li>
 *     <li>Parsing of all file contents into structured domain models.</li>
 * </ul>
 * </p>
 *
 * @author Victor Hugo Gaspar Quinn
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ReadAllCardData extends Transaction<CalypsoCardCDMX, ReaderPCSC> {

    private final WriteAccessLevel writeAccessLevel;

    private CalypsoCardCDMX cdmxCard;
    private TransactionResult<CalypsoCardCDMX> simpleRead;
    private TransactionResult<byte[]> readFile;
    private TransactionResult<SortedMap<Integer, byte[]>> readFiles;

    /**
     * Create an {@link ReadAllCardData} transaction, this transaction read all {@link CalypsoCard} data and parse
     * to {@link CalypsoCardCDMX}
     *
     * @param writeAccessLevel the level to read
     */
    public ReadAllCardData(WriteAccessLevel writeAccessLevel) {
        super("read card");
        this.writeAccessLevel = writeAccessLevel;
    }

    /**
     * Read all files card and parse to {@link CalypsoCardCDMX}, no verification of the data is performed
     *
     * @param reader the reader to use
     * @return the data parsed
     */
    @Override
    public TransactionResult<CalypsoCardCDMX> execute(ReaderPCSC reader) {

        simpleRead = reader.executeTransaction(new SimpleReadCard());

        if (!simpleRead.isOk())
            throw new ReaderException(simpleRead.getMessage());

        cdmxCard = simpleRead.getData();
        log.info("Reading all card data for '{}'", cdmxCard.getSerial());

        readEnvironmentFile(reader);
        readLogFiles(reader);
        readEventFiles(reader);
        readContractFiles(reader);

        return TransactionResult
                .<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(cdmxCard)
                .build();
    }

    private void readEnvironmentFile(ReaderPCSC reader) {
        readFile = reader.executeTransaction(
                new ReadCardFile(writeAccessLevel, CDMX.ENVIRONMENT_FILE, 1)
        );

        cdmxCard.setBalance(reader.getCalypsoCard().getSvBalance());

        if (readFile.isOk()) {
            cdmxCard.setEnvironment(new Environment(readFile.getData()));
        } else {
            log.warn("Failed to read environment file: {}", readFile.getMessage());
        }
    }

    private void readLogFiles(ReaderPCSC reader) {
        SvDebitLogRecord debitLogRecord = reader.getCalypsoCard().getSvDebitLogLastRecord();
        if (debitLogRecord != null) {
            cdmxCard.setDebitLog(new DebitLog(debitLogRecord));
        } else {
            log.warn("Failed to read debit log record");
        }

        SvLoadLogRecord loadLogRecord = reader.getCalypsoCard().getSvLoadLogRecord();
        if (loadLogRecord != null) {
            cdmxCard.setLoadLog(new LoadLog(loadLogRecord));
        } else {
            log.warn("Failed to read load log record");
        }
    }

    private void readEventFiles(ReaderPCSC reader) {
        readFiles = reader.executeTransaction(
                new ReadCardFilePartially(CDMX.EVENT_FILE, (byte) 1, (byte) 3, 0, 29)
        );

        if (!readFiles.isOk()) {
            log.warn("Failed to read events file: {}", readFiles.getMessage());
            return;
        }

        List<Event> events = new ArrayList<>();
        SortedMap<Integer, byte[]> partialEvents = readFiles.getData();
        for (var entry : partialEvents.entrySet()) {
            events.add(new Event(entry.getKey(), entry.getValue()));
        }
        cdmxCard.setEvents(events);
    }

    private void readContractFiles(ReaderPCSC reader) {
        readFiles = reader.executeTransaction(
                new ReadCardFilePartially((byte) 0x09, (byte) 1, (byte) 8, 0, 10)
        );

        if (!readFiles.isOk()) {
            log.warn("Failed to read contracts file: {}", readFiles.getMessage());
            return;
        }

        Contracts contracts = new Contracts();
        SortedMap<Integer, byte[]> partialContracts = readFiles.getData();
        for (var entry : partialContracts.entrySet()) {
            contracts.add(new Contract(entry.getKey(), entry.getValue()));
        }
        cdmxCard.setContracts(contracts);
    }
}
