package com.idear.devices.card.cardkit.core.io.transaction;

import com.idear.devices.card.cardkit.core.exception.CardKitException;
import com.idear.devices.card.cardkit.core.io.reader.AbstractReader;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Getter
@Slf4j
public abstract class AbstractTransactionManager<
        C extends AbstractReader,
        S extends AbstractReader, X extends AbstractTransactionContext> {
    protected final C cardReader;
    protected final S samReader;

    protected abstract X createContext();

    public <T> TransactionResult<T> execute(AbstractTransaction<T, X> abstractTransaction) {
        String nameTransaction = Strings.normalizeClassName(abstractTransaction.getClass().getSimpleName());
        long time = System.currentTimeMillis();

        try {
            log.debug("Executing transaction {}", nameTransaction);
            TransactionResult<T> result = abstractTransaction.execute(createContext());
            result.setTransactionName(nameTransaction);
            result.setTime(System.currentTimeMillis() - time);
            return result;

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
}
