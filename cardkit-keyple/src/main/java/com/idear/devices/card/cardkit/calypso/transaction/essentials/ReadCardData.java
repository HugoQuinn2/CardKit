package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.idear.devices.card.cardkit.core.datamodel.calypso.file.*;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;

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
public class ReadCardData extends Transaction<CalypsoCardCDMX, ReaderPCSC> {

    private final WriteAccessLevel writeAccessLevel;

    private CalypsoCardCDMX cdmxCard;
    private TransactionResult<byte[]> readFile;
    private TransactionResult<SortedMap<Integer, byte[]>> readFiles;

    /**
     * Create an {@link ReadCardData} transaction, this transaction read all {@link CalypsoCard} data and parse
     * to {@link CalypsoCardCDMX}
     *
     * @param writeAccessLevel the level to read
     */
    public ReadCardData(WriteAccessLevel writeAccessLevel, CalypsoCardCDMX calypsoCardCDMX) {
        super("read card");
        this.writeAccessLevel = writeAccessLevel;
        this.cdmxCard = calypsoCardCDMX;
    }

    /**
     * Read all files card and parse to {@link CalypsoCardCDMX}, no verification of the data is performed
     *
     * @param reader the reader to use
     * @return the data parsed
     */
    @Override
    public TransactionResult<CalypsoCardCDMX> execute(ReaderPCSC reader) {

        if (!reader.getCardReader().isCardPresent())
            throw new ReaderException("no card on reader");

        readEnvironmentFile(reader);
        readEventFiles(reader);
        readContractFiles(reader);

        return TransactionResult
                .<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(cdmxCard)
                .message("All card data '" + cdmxCard.getSerial() + "' was read")
                .build();
    }

    private void readEnvironmentFile(ReaderPCSC reader) {
        readFile = reader.execute(
                new ReadCardFile(writeAccessLevel, Calypso.ENVIRONMENT_FILE, 1)
        );

        if (readFile.isOk()) {
            cdmxCard.setEnvironment(new Environment().parse(readFile.getData()));
        } else {
            log.warn("failed to read environment file: {}", readFile.getMessage());
        }
    }

    private void readEventFiles(ReaderPCSC reader) {
        readFiles = reader.execute(
                new ReadCardFilePartially(Calypso.EVENT_FILE, (byte) 1, (byte) 3, 0, 29)
        );

        if (!readFiles.isOk()) {
            log.warn("Failed to read events file: {}", readFiles.getMessage());
            return;
        }

        Events events = new Events();
        SortedMap<Integer, byte[]> partialEvents = readFiles.getData();
        for (var entry : partialEvents.entrySet()) {
            events.add(new Event(entry.getKey()).parse(entry.getValue()));
        }
        cdmxCard.setEvents(events);
    }

    private void readContractFiles(ReaderPCSC reader) {
        readFiles = reader.execute(
                new ReadCardFilePartially(Calypso.CONTRACT_FILE, (byte) 1, (byte) 8, 0, 10)
        );

        if (!readFiles.isOk()) {
            log.warn("Failed to read contracts file: {}", readFiles.getMessage());
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
        cdmxCard.setContracts(contracts);
    }
}
