package com.idear.devices.card.cardkit.core.io.reader;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Base abstraction for a card reader device.
 * <p>
 * This class defines the common lifecycle and card presence handling
 * for all reader implementations. Concrete readers must implement
 * the connection logic and card communication behavior.
 * </p>
 *
 * <p>
 * The reader supports blocking wait operations for card insertion
 * and card removal, with optional timeout control.
 * </p>
 *
 * <p><b>Threading note:</b> Wait operations are blocking and should not be
 * executed on UI or main application threads.</p>
 */
@Slf4j
@Data
public abstract class AbstractReader {

    /** Indicates that the reader is waiting for a card to be presented. */
    private volatile boolean waitingForCardPresent;

    /** Indicates that the reader is waiting for a card to be removed. */
    private volatile boolean waitingForCardAbsent;

    /**
     * Enables the reader connection and initializes the communication channel.
     *
     * @throws Exception if an error occurs while starting the reader communication
     */
    abstract public void connect() throws Exception;

    /**
     * Closes the reader connection and releases all resources.
     */
    abstract public void disconnect();

    /**
     * Checks whether a card is currently present on the reader.
     *
     * @return {@code true} if a card is detected on the reader, {@code false} otherwise
     */
    abstract public boolean isCardOnReader();

    /**
     * Establishes a communication session with the card currently present
     * on the reader.
     */
    abstract public void connectToCard();

    /**
     * Terminates the communication session with the card.
     */
    abstract public void disconnectFromCard();

    /**
     * Send a command to card
     *
     * @param command to send
     * @return the apdu response
     */
    abstract public ResponseAPDU simpleCommand(CommandAPDU command);

    /**
     * Blocks execution until a card is presented on the reader or the timeout
     * expires.
     *
     * @param l maximum waiting time in milliseconds;
     *                      if {@code 0} or negative, waits indefinitely
     */
    public void waitForCardPresent(long l) {
        long start = System.currentTimeMillis();
        waitingForCardPresent = true;
        while (waitingForCardPresent) {
            if (isCardOnReader())
                break;

            if (l > 0 &&
                    (System.currentTimeMillis() - start) >= l)
                break;

            sleep();
        }
        connectToCard();
        waitingForCardPresent = false;
    }

    /**
     * Blocks execution until the card is removed from the reader or the timeout
     * expires.
     *
     * @param l maximum waiting time in milliseconds;
     *                      if {@code 0} or negative, waits indefinitely
     */
    public void waitForCarAbsent(long l) {
        long start = System.currentTimeMillis();
        waitingForCardAbsent = true;
        while (waitingForCardAbsent) {
            if (!isCardOnReader())
                break;

            if (l > 0 &&
                    (System.currentTimeMillis() - start) >= l)
                break;

            sleep();
        }
        disconnectFromCard();
        waitingForCardAbsent = false;
    }

    /**
     * Stops any ongoing wait operation immediately.
     * This method can be safely called from another thread.
     */
    public void stopWaiting() {
        waitingForCardPresent = false;
        waitingForCardAbsent = false;
    }

    /**
     * Sleeps for a short polling interval.
     */
    protected void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

}
