package com.idear.devices.card.cardkit.core.io.reader;

import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

public abstract class Reader<E> {

    private final List<ReaderEvent<E>> readerEvents = new ArrayList<>();

    abstract public void init() throws Exception;
    abstract public void disconnect();

    /**
     * Execute a {@link Transaction} using actual {@link Reader} params.
     *
     * @param transaction the transaction to execute
     * @return the result transaction
     */
    public <T, R extends Reader<E>> TransactionResult<T> executeTransaction(Transaction<T, R> transaction) {
        try {
            long time = System.currentTimeMillis();
            TransactionResult<T> transactionResult = transaction.execute((R) this);
            transactionResult.setTime(System.currentTimeMillis() - time);
            return transactionResult;
        } catch (Exception e) {
            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ERROR)
                    .message(e.getMessage())
                    .build();
        }
    }

    public void addListeners(ReaderEvent e) {
        readerEvents.add(e);
    }

    @SuppressWarnings("unchecked")
    public void fireEvent(E data) {
        for (ReaderEvent<E> event : readerEvents) {
            event.onEvent(data);
        }
    }
}
