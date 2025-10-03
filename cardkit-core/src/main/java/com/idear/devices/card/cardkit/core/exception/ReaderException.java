package com.idear.devices.card.cardkit.core.exception;

public class ReaderException extends RuntimeException {
    public ReaderException(String message) {
        super(message);
    }

    public ReaderException(String message, Object... o) {
        super(String.format(message, o));
    }
}
