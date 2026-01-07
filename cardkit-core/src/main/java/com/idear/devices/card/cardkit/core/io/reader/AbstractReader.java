package com.idear.devices.card.cardkit.core.io.reader;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public abstract class AbstractReader {

    private boolean waitingForCardPresent;
    private boolean waitingForCardAbsent;

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

    public void waitForCardPresent(long l) {
        long start = System.currentTimeMillis();
        waitingForCardPresent = true;
        while (waitingForCardPresent) {
            if (isCardOnReader())
                break;

            if (l > 0 && (System.currentTimeMillis() - start) >= l)
                break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        connectToCard();
        waitingForCardPresent = false;
    }

    public void waitForCarAbsent(long l) {
        long start = System.currentTimeMillis();
        waitingForCardAbsent = true;
        while (waitingForCardAbsent) {
            if (!isCardOnReader())
                break;

            if (l > 0 && (System.currentTimeMillis() - start) >= l)
                break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        disconnectFromCard();
        waitingForCardAbsent = false;
    }

}
