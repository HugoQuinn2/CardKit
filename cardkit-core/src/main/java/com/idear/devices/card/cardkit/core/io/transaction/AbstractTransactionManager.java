package com.idear.devices.card.cardkit.core.io.transaction;

import com.idear.devices.card.cardkit.core.exception.CardKitException;
import com.idear.devices.card.cardkit.core.io.reader.AbstractReader;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Getter
@Slf4j
public abstract class AbstractTransactionManager<
        C extends AbstractReader,
        S extends AbstractReader, X extends AbstractTransactionContext> {

    private final ExecutorService executorCardMonitor = Executors.newSingleThreadExecutor();
    private final ExecutorService executorCardEvent = Executors.newSingleThreadExecutor();
    private final List<ICardEvent> cardEventList = new ArrayList<>();
    private boolean cardMonitor;

    protected final C cardReader;
    protected final S samReader;

    protected abstract void onCardPresent();
    protected abstract void onCardAbsent();
    protected abstract X createContext();

    /**
     * Start a loop that verify if the card is present/absent and dispatch the corresponding {@link CardStatus}.
     */
    private void cardMonitor() {
        while (cardMonitor) {
            try {
                cardReader.waitForCardPresent(0);
                onCardPresent();
                notifyListeners(CardStatus.CARD_PRESENT);
                cardReader.waitForCarAbsent(0);
                onCardAbsent();
                notifyListeners(CardStatus.CARD_ABSENT);
            } catch (Exception e) {
                log.error("card monitor error", e);
            }
        }
    }

    private void notifyListeners(CardStatus cardStatus) {
        for (ICardEvent cardEvent : cardEventList)
            executorCardEvent.submit(() -> cardEvent.onEvent(cardStatus));
    }

    public void startCardMonitor() {
        cardMonitor = true;
        executorCardMonitor.submit(this::cardMonitor);
    }

    public void stopCardMonitor() {
        cardReader.setWaitingForCardAbsent(false);
        cardReader.setWaitingForCardPresent(false);
        cardMonitor = false;
        executorCardMonitor.shutdown();
    }

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
