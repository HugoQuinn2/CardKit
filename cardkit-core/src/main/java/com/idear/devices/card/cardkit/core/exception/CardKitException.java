package com.idear.devices.card.cardkit.core.exception;

public class CardKitException extends RuntimeException {

    public CardKitException(String message) {
        super(message);
    }

    public CardKitException(String message, Throwable cause) {
        super(message, cause);
    }

}
