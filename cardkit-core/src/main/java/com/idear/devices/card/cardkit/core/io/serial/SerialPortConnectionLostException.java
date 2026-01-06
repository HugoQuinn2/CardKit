package com.idear.devices.card.cardkit.core.io.serial;

public class SerialPortConnectionLostException extends RuntimeException {
    public SerialPortConnectionLostException(String message) {
        super(message);
    }
}
