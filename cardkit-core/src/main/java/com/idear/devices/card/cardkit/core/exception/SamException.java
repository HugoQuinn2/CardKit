package com.idear.devices.card.cardkit.core.exception;

public class SamException extends CardKitException {
    public SamException(String message) {
        super(message);
    }
    public SamException(String message, Object... o) {
        this(String.format(message, 0));
    }
}
