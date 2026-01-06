package com.idear.devices.card.cardkit.core.io.reader;

import com.idear.devices.card.cardkit.core.exception.CardKitException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractReader {

    /**
     * Enable connection with the reader and start communication chanel
     *
     * @throws Exception if an error occurred starting communication reader
     */
    abstract public void connect() throws Exception;

    abstract public void disconnect();
    abstract public boolean isCardOnReader();
    abstract public void connectToCard();
    abstract public void disconnectFromCard();

    abstract public void waitForCardPresent(long l);
    abstract public void waitForCarAbsent(long l);

//    /**
//     * Execute a {@link AbstractTransaction} using actual {@link AbstractReader} params.
//     *
//     * @param abstractTransaction the transaction to execute
//     * @return the result transaction
//     */
//    public <T, R extends AbstractReader> TransactionResult<T> execute(AbstractTransaction<T, R> abstractTransaction) {
//        String nameTransaction = Strings.normalizeClassName(abstractTransaction.getClass().getSimpleName());
//        long time = System.currentTimeMillis();
//        try {
//            if (!this.isCardOnReader())
//                throw new ReaderException("no card on reader");
//
//            log.debug("Executing transaction {}", nameTransaction);
//            TransactionResult<T> transactionResult = abstractTransaction.execute((R) this);
//            transactionResult.setTime(System.currentTimeMillis() - time);
//            transactionResult.setTransactionName(nameTransaction);
//            return transactionResult;
//        } catch (CardKitException aborted) {
//            return TransactionResult.<T>builder()
//                    .transactionStatus(TransactionStatus.ABORTED)
//                    .transactionName(nameTransaction)
//                    .message(aborted.getMessage())
//                    .exception(aborted)
//                    .time(System.currentTimeMillis() - time)
//                    .build();
//        } catch (Throwable e) {
//            log.error("{}: {} - {}", nameTransaction, e.getClass().getSimpleName(), e.getMessage());
//            return TransactionResult.<T>builder()
//                    .transactionStatus(TransactionStatus.ERROR)
//                    .transactionName(nameTransaction)
//                    .message(e.getMessage())
//                    .exception(e)
//                    .time(System.currentTimeMillis() - time)
//                    .build();
//        }
//    }

}
