package com.idear.devices.card.cardkit.core.io.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.exception.CardKitException;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.*;

/**
 * Represents the result of a transaction execution within the CardKit system.
 * <p>
 * This class encapsulates details about a transaction, such as its name,
 * execution status, result data, message, and duration.
 * It also provides utility methods for handling transaction outcomes,
 * including condition-based exception throwing and status checks.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 *
 * // basic method, slower but better control of the process
 * TransactionResult<CardData> resultCardData = reader.execute(new ReadAllCardData());
 * if (!resultCardData.isOk)
 *  throw new RuntimeException(resultCardData.getMessage());
 *
 * CardData cardData = resultData.getData();
 *
 * // cleaner method, throw a exception and stop process if the status is different to OK
 * CardData cardData = reader
 *  .execute(new ReadAllCardData())
 *  .throwMessageOnError(ReaderException.class)
 *  .getData();
 *
 * }</pre>
 *
 * @param <T> The type of data returned by the transaction (e.g., card data object, string response, etc.).
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Builder
@Getter
@Setter
@ToString
public class TransactionResult<T> extends Item {

    /** The descriptive name of the transaction executed. */
    private String transactionName;

    /** The status of the transaction (OK, ERROR, ABORTED, etc.). */
    private TransactionStatus transactionStatus;

    /** The data object produced by the transaction, if available. */
    private T data;

    /** A human-readable message providing context about the transaction result. */
    private String message;

    /** The execution time of the transaction in milliseconds. */
    private long time;

    /** The exception cached if apply */
    private Throwable exception;

    /**
     * Checks whether the current transaction has the specified status.
     *
     * @param transactionStatus The status to compare against.
     * @return {@code true} if the transaction matches the given status, otherwise {@code false}.
     */
    @JsonIgnore
    public boolean is(TransactionStatus transactionStatus) {
        return this.transactionStatus.equals(transactionStatus);
    }

    /**
     * Checks if the transaction completed successfully.
     *
     * @return {@code true} if the transaction status is {@link TransactionStatus#OK}, otherwise {@code false}.
     */
    @JsonIgnore
    public boolean isOk() {
        return is(TransactionStatus.OK);
    }

    /**
     * Prints the JSON representation of this transaction result to the console.
     *
     * @return The same {@code TransactionResult} instance, allowing method chaining.
     */
    @JsonIgnore
    public TransactionResult<T> print() {
        System.out.println(this.toJson());
        return this;
    }

    /**
     * Throws an exception if the transaction status is not OK.
     * <p>
     * The provided exception class must have a constructor that accepts a {@code String} message.
     * </p>
     *
     * @param exceptionClass The type of exception to throw if the transaction failed.
     * @return The same {@code TransactionResult} instance for chaining.
     * @throws CardKitException If reflection fails or no suitable constructor is found.
     */
    @JsonIgnore
    public TransactionResult<T> throwMessageOnError(Class<? extends Exception> exceptionClass) {
        if (is(TransactionStatus.ERROR))
            try {
                throw (RuntimeException) exceptionClass.getConstructor(String.class)
                        .newInstance(message);
            } catch (ReflectiveOperationException e) {
                throw new CardKitException(
                        message
                );
            }

        return this;
    }

    /**
     * Throws an exception if the transaction status is OK.
     * <p>
     * Useful for enforcing specific post-conditions or validating unexpected success cases.
     * </p>
     *
     * @param exceptionClass The type of exception to throw if the transaction succeeded.
     * @return The same {@code TransactionResult} instance for chaining.
     * @throws CardKitException If reflection fails or no suitable constructor is found.
     */
    @JsonIgnore
    public TransactionResult<T> throwMessageOnOk(Class<? extends CardKitException> exceptionClass) {
        if (isOk())
            try {
                throw (RuntimeException) exceptionClass.getConstructor(String.class)
                        .newInstance(message);
            } catch (ReflectiveOperationException e) {
                throw new CardKitException(
                        message
                );
            }

        return this;
    }

    /**
     * Always throws the given exception, regardless of transaction status.
     * <p>
     * The provided exception class must have a constructor that accepts a {@code String} message.
     * </p>
     *
     * @param exceptionClass The type of exception to throw.
     * @throws CardKitException If reflection fails or no suitable constructor is found.
     */
    @JsonIgnore
    public void throwMessage(Class<? extends CardKitException> exceptionClass) {
        try {
            throw (RuntimeException) exceptionClass.getConstructor(String.class)
                    .newInstance(message);
        } catch (ReflectiveOperationException e) {
            throw new CardKitException(
                    message
            );
        }
    }

    /**
     * Throws an exception if the transaction status is {@link TransactionStatus#ABORTED}.
     *
     * @param exceptionClass The type of exception to throw if the transaction was aborted.
     * @return The same {@code TransactionResult} instance for chaining.
     * @throws CardKitException If reflection fails or no suitable constructor is found.
     */
    @JsonIgnore
    public TransactionResult<T> throwMessageOnAborted(Class<? extends CardKitException> exceptionClass) {
        if (is(TransactionStatus.ABORTED))
            try {
                throw (RuntimeException) exceptionClass.getConstructor(String.class)
                        .newInstance(message);
            } catch (ReflectiveOperationException e) {
                throw new CardKitException(
                        message
                );
            }

        return this;
    }

    public TransactionResult<T> throwException() throws Throwable {
        if (exception != null)
            throw exception;

        return this;
    }

}
