package com.idear.devices.card.cardkit.core.io.reader;

import com.idear.devices.card.cardkit.core.exception.CardKitException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Reader<E> {

    private final List<ReaderEvent<E>> readerEvents = new ArrayList<>();

    abstract public void connect() throws Exception;
    abstract public void disconnect();
    abstract public boolean isCardOnReader();
    abstract public void connectToCard();
    abstract public void disconnectFromCard();
    abstract public void waitForCardPresent(long l);
    abstract public void waitForCarAbsent(long l);

    /**
     * Execute a {@link Transaction} using actual {@link Reader} params.
     *
     * @param transaction the transaction to execute
     * @return the result transaction
     */
    public <T, R extends Reader<E>> TransactionResult<T> execute(Transaction<T, R> transaction) {
        String nameTransaction = Strings.normalizeClassName(transaction.getClass().getSimpleName());
        long time = System.currentTimeMillis();
        try {
            if (!this.isCardOnReader())
                throw new ReaderException("no card on reader");

            log.debug("Executing transaction {}", nameTransaction);
            TransactionResult<T> transactionResult = transaction.execute((R) this);
            transactionResult.setTime(System.currentTimeMillis() - time);
            transactionResult.setTransactionName(nameTransaction);
            return transactionResult;
        } catch (CardKitException aborted) {
            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ABORTED)
                    .transactionName(nameTransaction)
                    .message(aborted.getMessage())
                    .exception(aborted)
                    .time(System.currentTimeMillis() - time)
                    .build();
        } catch (Throwable e) {
            log.error("{}: {} - {}", nameTransaction, e.getClass().getSimpleName(), e.getMessage());
            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ERROR)
                    .transactionName(nameTransaction)
                    .message(e.getMessage())
                    .exception(e)
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
