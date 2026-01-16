package com.idear.devices.card.cardkit.core.exception;

import lombok.Getter;

@Getter
public class ApduResponseException extends CardKitException {
    private final int sw;

    public ApduResponseException(String message, int sw) {
        super(message);
        this.sw = sw;
    }
    public ApduResponseException(String message, int sw, Throwable cause) {
        super(message, cause);
        this.sw = sw;
    }

}
