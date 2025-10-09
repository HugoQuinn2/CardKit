package com.idear.devices.card.cardkit.core.io.transaction;

import com.idear.devices.card.cardkit.core.io.reader.Reader;
import lombok.Data;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Represents a generic transaction operation executed using a specific {@link Reader}.
 * <p>
 * This class supports reporting progress via a lambda consumer,
 * which can be used to track execution state (from 0 to 100).
 *
 * @param <T> the type of result returned by this transaction
 * @param <R> the type of reader used to execute the transaction
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
@Data
public abstract class Transaction<T, R extends Reader<?>> {

    /** The name or identifier of this transaction. */
    private final String name;

    /**
     * Optional progress listener. It receives values between 0 and 100
     * indicating the current progress of the transaction with a message.
     */
    private Consumer<ProgressUpdate> progressListener = update -> {};

    /**
     * Sets a progress listener to track the transaction progress.
     *
     * @param listener a consumer receiving values from 0 to 100
     * @return this transaction (for chaining)
     */
    public Transaction<T, R> onProgress(Consumer<ProgressUpdate> listener) {
        this.progressListener = listener != null ? listener : update -> {};
        return this;
    }

    /**
     * Reports a progress value to the listener.
     * The value will be automatically clamped between 0 and 100.
     *
     * @param percent the current progress percentage
     * @param message the message
     */
    protected void reportProgress(int percent, String message) {
        if (progressListener != null) {
            progressListener.accept(new ProgressUpdate(
                    Math.max(0, Math.min(100, percent)),
                    message != null ? message : ""
            ));
        }
    }

    /**
     * Executes this transaction using the given reader.
     *
     * @param reader the reader used to perform the transaction
     * @return the transaction result
     */
    public abstract TransactionResult<T> execute(R reader);

}
