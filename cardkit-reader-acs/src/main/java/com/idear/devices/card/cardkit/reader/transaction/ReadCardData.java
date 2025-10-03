package com.idear.devices.card.cardkit.reader.transaction;

import com.idear.devices.card.cardkit.core.io.reader.Reader;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.reader.CalypsoCDMXCard;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ReadCardTransaction extends Transaction<CalypsoCDMXCard> {

    private final WriteAccessLevel writeAccessLevel;

    public ReadCardTransaction(WriteAccessLevel writeAccessLevel) {
        super("read card");
        this.writeAccessLevel = writeAccessLevel;
    }

    @Override
    public TransactionResult<CalypsoCDMXCard> execute(Reader reader) {

        TransactionResult<CalypsoCDMXCard> simpleRead = reader.executeTransaction(
                new SimpleReadCard()
        );

        CalypsoCDMXCard calypsoCDMXCard = simpleRead.getData();

        // Read environment file (0x07) and parse
        TransactionResult<CalypsoCard> readFile = reader.executeTransaction(
                new ReadCardFile(writeAccessLevel, (byte) 0x07, 1)
        );

        calypsoCDMXCard.setEnvironment(
                new CalypsoCDMXCard.Environment(
                        readFile.getData()
                                .getFileBySfi((byte) 0x07)
                                .getData()
                                .getContent()
                )
        );

        // Read events log file (0x08) and parse
        readFile = reader.executeTransaction(
                new ReadCardFilePartially((byte) 0x08, (byte) 1, (byte) 3,0, 10)
        );

        List<CalypsoCDMXCard.Event> events = new ArrayList<>();
        SortedMap<Integer, byte[]> partialEvents = readFile.getData()
                .getFileBySfi((byte) 0x08)
                .getData()
                .getAllRecordsContent();

        for (int id : partialEvents.keySet()) {
            events.add(new CalypsoCDMXCard.Event(id, partialEvents.get(id)));
        }

        calypsoCDMXCard.setEvents(events);

        // Read contracts and parse (0x09)
        CalypsoCDMXCard.Contracts contracts = new CalypsoCDMXCard.Contracts();

        readFile = reader.executeTransaction(
                new ReadCardFilePartially((byte) 0x09, (byte) 1, (byte) 8, 0, 10)
        );

        SortedMap<Integer, byte[]> partialContracts = readFile.getData()
                .getFileBySfi((byte) 0x09)
                .getData()
                .getAllRecordsContent();

        for (int id : partialContracts.keySet()) {
            contracts.add(new CalypsoCDMXCard.Contract(id, partialContracts.get(id)));
        }

        calypsoCDMXCard.setContracts(contracts);


        log.info("Reading all card data '{}'", calypsoCDMXCard.getSerial());
        return TransactionResult
                .<CalypsoCDMXCard>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCDMXCard)
                .build();
    }

}
