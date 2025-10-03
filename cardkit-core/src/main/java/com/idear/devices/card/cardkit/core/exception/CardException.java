package com.idear.devices.card.cardkit.core.exception;

public class CardException extends RuntimeException {
    public CardException(String message) {
        super(message);
    }

    public CardException(String message, Object... o) {
        super(String.format(message, o));
    }
}
