package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.calypso.file.Events;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import lombok.Getter;
import lombok.var;

import java.util.SortedMap;

@Getter
public class SaveEvent extends Transaction<Boolean, ReaderPCSC> {

    private final TransactionType transactionType;
    private final int locationId;
    private final int amount;

    private Events events;

    public SaveEvent(
            TransactionType transactionType,
            int locationId,
            int amount) {
        super("save event");
        this.transactionType = transactionType;
        this.locationId = locationId;
        this.amount = amount;
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

        if (!reader.getCardReader().isCardPresent()) {
            throw new ReaderException("No card on reader");
        }

        readEventFiles(reader);

        // Build event
        Event event = Event.builEvent(
                transactionType,
                reader.getCalypsoSam(),
                events.getNextTransactionNumber(),
                locationId,
                amount
        );

        // prepare write event
        if (transactionType.isWritten()) {
            reader.getCardTransactionManager()
                    .prepareAppendRecord(event.getFileId(), event.unparse());
        }

        // Fire event on reader if the transaction is reported
        if (transactionType.isReported())
            reader.fireCardEvent(event);

        return null;
    }

    private void readEventFiles(ReaderPCSC reader) {
        TransactionResult<SortedMap<Integer, byte[]>> readFiles = reader.execute(
                new ReadCardFilePartially(CDMX.EVENT_FILE, (byte) 1, (byte) 3, 0, 29)
        );

        if (!readFiles.isOk())
            throw new CardException("Error reading card events");

        Events events = new Events();
        SortedMap<Integer, byte[]> partialEvents = readFiles.getData();
        for (var entry : partialEvents.entrySet()) {
            events.add(new Event(entry.getKey()).parse(entry.getValue()));
        }

        this.events = events;
    }

}
