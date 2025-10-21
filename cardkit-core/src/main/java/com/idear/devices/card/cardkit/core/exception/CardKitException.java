package com.idear.devices.card.cardkit.core.exception;

public class CardKitException extends RuntimeException {

    public CardKitException(String message) {
        super(message);
    }

    public CardKitException(String message, Object... o) {
        super(String.format(message, o));
    }

}
