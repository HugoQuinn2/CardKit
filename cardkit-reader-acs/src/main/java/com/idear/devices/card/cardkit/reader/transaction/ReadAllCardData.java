package com.idear.devices.card.cardkit.reader.transaction;

import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.reader.CalypsoCDMXCard;
import com.idear.devices.card.cardkit.reader.ReaderPCSC;
import com.idear.devices.card.cardkit.reader.file.DebitLog;
import com.idear.devices.card.cardkit.reader.file.LoadLog;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.SvDebitLogRecord;
import org.eclipse.keypop.calypso.card.card.SvLoadLogRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ReadAllCardData extends Transaction<CalypsoCDMXCard, ReaderPCSC> {

    private final WriteAccessLevel writeAccessLevel;

    private CalypsoCDMXCard  cdmxCard;
    private TransactionResult<CalypsoCDMXCard> simpleRead;
    private TransactionResult<CalypsoCard> readFile;

    /**
     * Create an {@link ReadAllCardData} transaction, this transaction read all {@link CalypsoCard} data and parse
     * to {@link CalypsoCDMXCard}
     *
     * @param writeAccessLevel the level to read
     */
    public ReadAllCardData(WriteAccessLevel writeAccessLevel) {
        super("read card");
        this.writeAccessLevel = writeAccessLevel;
    }

    /**
     * Read all files card and parse to {@link CalypsoCDMXCard}, no verification of the data is performed
     *
     * @param reader the reader to use
     * @return the data parsed
     */
    @Override
    public TransactionResult<CalypsoCDMXCard> execute(ReaderPCSC reader) {

        simpleRead = reader.executeTransaction(new SimpleReadCard());

        if (!simpleRead.is(TransactionStatus.OK))
            throw new ReaderException(simpleRead.getMessage());

        cdmxCard = simpleRead.getData();
        log.info("Reading all card data '{}'", cdmxCard.getSerial());

        // Read environment file (0x07) and parse
        readFile = reader.executeTransaction(
                new ReadCardFile(writeAccessLevel, CDMX.ENVIRONMENT_FILE, 1)
        );

        cdmxCard.setBalance(reader.getCalypsoCard().getSvBalance());

        if (readFile.is(TransactionStatus.OK)) {
            cdmxCard.setEnvironment(
                    new CalypsoCDMXCard.Environment(
                            readFile.getData()
                                    .getFileBySfi(CDMX.ENVIRONMENT_FILE)
                                    .getData()
                                    .getContent()
                    )
            );
        } else {
            log.warn("The environment file could not be read: {}", readFile.getMessage());
        }

        // Read debit log file (0x15) and parse
        SvDebitLogRecord svDebitLogRecord = reader.getCalypsoCard().getSvDebitLogLastRecord();
        if (svDebitLogRecord != null) {
            cdmxCard.setDebitLog(new DebitLog(svDebitLogRecord));
        } else {
            log.warn("The debit log file could not be read");
        }

        // Read load log file (0x14) and parse
        SvLoadLogRecord svLoadLogRecord = reader.getCalypsoCard().getSvLoadLogRecord();
        if (svDebitLogRecord != null) {
            cdmxCard.setLoadLog(new LoadLog(svLoadLogRecord));
        } else {
            log.warn("The load log file could not be read");
        }


        // Read events log file (0x08) and parse
        readFile = reader.executeTransaction(
                new ReadCardFilePartially(CDMX.EVENT_FILE, (byte) 1, (byte) 3,0, 29)
        );

        if (readFile.is(TransactionStatus.OK)) {
            List<CalypsoCDMXCard.Event> events = new ArrayList<>();
            SortedMap<Integer, byte[]> partialEvents = readFile.getData()
                    .getFileBySfi(CDMX.EVENT_FILE)
                    .getData()
                    .getAllRecordsContent();

            for (int id : partialEvents.keySet()) {
                events.add(new CalypsoCDMXCard.Event(id, partialEvents.get(id)));
            }

            cdmxCard.setEvents(events);
        } else {
            log.warn("The events file could not be read: {}", readFile.getMessage());
        }

        // Read contracts and parse (0x09)
        readFile = reader.executeTransaction(
                new ReadCardFilePartially((byte) 0x09, (byte) 1, (byte) 8, 0, 10)
        );

        if (readFile.is(TransactionStatus.OK)) {
            CalypsoCDMXCard.Contracts contracts = new CalypsoCDMXCard.Contracts();

            SortedMap<Integer, byte[]> partialContracts = readFile.getData()
                    .getFileBySfi((byte) 0x09)
                    .getData()
                    .getAllRecordsContent();

            for (int id : partialContracts.keySet()) {
                contracts.add(new CalypsoCDMXCard.Contract(id, partialContracts.get(id)));
            }

            cdmxCard.setContracts(contracts);
        } else {
            log.warn("The contracts file could not be read: {}", readFile.getMessage());
        }

        return TransactionResult
                .<CalypsoCDMXCard>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(cdmxCard)
                .build();
    }
}
