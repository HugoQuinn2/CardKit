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

/**
 * Base transaction manager responsible for coordinating card and SAM readers,
 * monitoring card presence events, and executing transactions.
 *
 * <p>
 * This manager starts a background monitoring loop that detects card insertion
 * and removal events and dispatches them asynchronously to registered listeners.
 * </p>
 *
 * <p>
 * Two independent reader devices are supported:
 * </p>
 * <ul>
 *   <li><b>Card reader</b>: used for passenger or media cards</li>
 *   <li><b>SAM reader</b>: used for secure access module operations</li>
 * </ul>
 *
 * <p><b>Threading model:</b></p>
 * <ul>
 *   <li>Card monitoring runs on a dedicated single-thread executor</li>
 *   <li>Event notifications are dispatched asynchronously</li>
 * </ul>
 *
 * @param <C> concrete type of the card reader
 * @param <S> concrete type of the SAM reader
 * @param <X> transaction context type
 */
@RequiredArgsConstructor
@Getter
@Slf4j
public abstract class AbstractTransactionManager<
        C extends AbstractReader,
        S extends AbstractReader,
        X extends AbstractTransactionContext> {

    /** Executor responsible for monitoring card presence changes. */
    private final ExecutorService executorCardMonitor = Executors.newSingleThreadExecutor();

    /** Executor responsible for dispatching card events to listeners. */
    private final ExecutorService executorCardEvent = Executors.newSingleThreadExecutor();

    /** Registered listeners interested in card status events. */
    private final List<ICardEvent> cardEventList = new ArrayList<>();

    /** Controls the lifecycle of the card monitoring loop. */
    private volatile boolean cardMonitor;

    /** Executor responsible for monitoring sam presence changes. */
    private final ExecutorService executorSamMonitor = Executors.newSingleThreadExecutor();

    /** Executor responsible for dispatching sam events to listeners. */
    private final ExecutorService executorSamEvent = Executors.newSingleThreadExecutor();

    /** Registered listeners interested in sam status events. */
    private final List<ICardEvent> samEventList = new ArrayList<>();

    /** Controls the lifecycle of the sam monitoring loop. */
    private volatile boolean samMonitor;

    /** Reader used to detect and communicate with cards. */
    protected final C cardReader;

    /** Reader used to detect and communicate with the SAM. */
    protected final S samReader;

    /**
     * Callback invoked when a card is detected on the reader.
     */
    protected abstract void onCardPresent();

    /**
     * Callback invoked when the card is removed from the reader.
     */
    protected abstract void onCardAbsent();

    /**
     * Callback invoked when a sam is detected on the reader.
     */
    protected abstract void onSamPresent();

    /**
     * Callback invoked when the sam is removed from the reader.
     */
    protected abstract void onSamAbsent();

    /**
     * Creates a new transaction context for each transaction execution.
     *
     * @return a new transaction context instance
     */
    protected abstract X createContext();

    /**
     * Background loop that monitors card presence and absence events
     * and dispatches the corresponding {@link CardStatus} notifications.
     */
    private void cardMonitor() {
        while (cardMonitor) {
            try {
                cardReader.waitForCardPresent(0);
                onCardPresent();
                notifyCardListeners(CardStatus.CARD_PRESENT);

                cardReader.waitForCarAbsent(0);
                onCardAbsent();
                notifyCardListeners(CardStatus.CARD_ABSENT);

            } catch (Exception e) {
                log.error("Card monitor error", e);
            }
        }
    }

    /**
     * Background loop that monitors sam presence and absence events
     * and dispatches the corresponding {@link CardStatus} notifications.
     */
    private void samMonitor() {
        while (samMonitor) {
            try {
                samReader.waitForCardPresent(0);
                onSamPresent();
                notifySamListeners(CardStatus.CARD_PRESENT);

                samReader.waitForCarAbsent(0);
                onSamAbsent();
                notifySamListeners(CardStatus.CARD_ABSENT);

            } catch (Exception e) {
                log.error("Sam monitor error", e);
            }
        }
    }

    /**
     * Dispatches a card status event to all registered listeners asynchronously.
     *
     * @param cardStatus the detected card status
     */
    private void notifyCardListeners(CardStatus cardStatus) {
        for (ICardEvent cardEvent : cardEventList) {
            executorCardEvent.submit(() -> cardEvent.onEvent(cardStatus));
        }
    }

    /**
     * Dispatches a sam status event to all registered listeners asynchronously.
     *
     * @param cardStatus the detected card status
     */
    private void notifySamListeners(CardStatus cardStatus) {
        for (ICardEvent cardEvent : samEventList) {
            executorSamEvent.submit(() -> cardEvent.onEvent(cardStatus));
        }
    }

    /**
     * Starts the card monitoring loop.
     */
    public void startCardMonitor() {
        cardMonitor = true;
        executorCardMonitor.submit(this::cardMonitor);
    }

    /**
     * Starts the sam monitoring loop.
     */
    public void startSamMonitor() {
        samMonitor = true;
        executorSamMonitor.submit(this::samMonitor);
    }

    /**
     * Stops the card monitoring loop and releases monitoring resources.
     */
    public void stopCardMonitor() {
        cardReader.stopWaiting();
        cardMonitor = false;
        executorCardMonitor.shutdown();
    }

    /**
     * Stops the sam monitoring loop and releases monitoring resources.
     */
    public void stopSamMonitor() {
        samReader.stopWaiting();
        samMonitor = false;
        executorSamMonitor.shutdown();
    }

    /**
     * Executes a transaction using a newly created transaction context.
     *
     * @param abstractTransaction the transaction to execute
     * @param <T> transaction result payload type
     * @return the transaction execution result
     */
    public <T> TransactionResult<T> execute(AbstractTransaction<T, X> abstractTransaction) {
        String transactionName =
                Strings.normalizeClassName(abstractTransaction.getClass().getSimpleName());

        long start = System.currentTimeMillis();

        try {
            log.debug("Executing transaction {}", transactionName);
            TransactionResult<T> result =
                    abstractTransaction.execute(createContext());

            result.setTransactionName(transactionName);
            result.setTime(System.currentTimeMillis() - start);
            return result;

        } catch (CardKitException aborted) {
            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ABORTED)
                    .transactionName(transactionName)
                    .message(aborted.getMessage())
                    .exception(aborted)
                    .time(System.currentTimeMillis() - start)
                    .build();

        } catch (Throwable e) {
            log.error("{}: {} - {}", transactionName,
                    e.getClass().getSimpleName(), e.getMessage());

            return TransactionResult.<T>builder()
                    .transactionStatus(TransactionStatus.ERROR)
                    .transactionName(transactionName)
                    .message(e.getMessage())
                    .exception(e)
                    .time(System.currentTimeMillis() - start)
                    .build();
        }
    }
}

