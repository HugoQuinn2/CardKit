package com.idear.devices.card.cardkit.core.io.reader;

import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.CardKitException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Reader<E> {

    private final List<ReaderEvent<E>> readerEvents = new ArrayList<>();

    abstract public void init() throws Exception;
    abstract public void disconnect();
    abstract public boolean isCardOnReader();

    /**
     * Execute a {@link Transaction} using actual {@link Reader} params.
     *
     * @param transaction the transaction to execute
     * @return the result transaction
     */
    public <T, R extends Reader<E>> TransactionResult<T> execute(Transaction<T, R> transaction) {
        long time = System.currentTimeMillis();
        try {
            if (!this.isCardOnReader())
                throw new ReaderException("no card on reader");

            log.debug("Executing transaction {}", transaction.getName());
            TransactionResult<T> transactionResult = transaction.execute((R) this);
            transactionResult.setTime(System.currentTimeMillis() - time);
            transactionResult.setTransactionName(transaction.getName());
            return transactionResult;
        } catch (CardKitException aborted) {
            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ABORTED)
                    .transactionName(transaction.getName())
                    .message(aborted.getMessage())
                    .time(System.currentTimeMillis() - time)
                    .build();
        } catch (Throwable e) {
            log.error("{}: {} - {}", transaction.getName(), e.getClass().getSimpleName(), e.getMessage());
            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ERROR)
                    .transactionName(transaction.getName())
                    .message(e.getMessage())
                    .time(System.currentTimeMillis() - time)
                    .build();
        }
    }

    public void addListeners(ReaderEvent<E> e) {
        readerEvents.add(e);
    }

    @SuppressWarnings("unchecked")
    public void fireEvent(E data) {
        for (ReaderEvent<E> event : readerEvents) {
            event.onEvent(data);
        }
    }
}
