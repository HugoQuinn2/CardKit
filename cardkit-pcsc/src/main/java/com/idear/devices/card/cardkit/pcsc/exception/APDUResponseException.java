package com.idear.devices.card.cardkit.pcsc.exception;

import lombok.Getter;

import javax.smartcardio.ResponseAPDU;

@Getter
public class APDUResponseException extends RuntimeException {
    private final int sw;
    public static final String DEFAULT_MESSAGE = "";

    public APDUResponseException(String message, int sw, Throwable cause) {
        super(message, cause);
        this.sw = sw;
    }

    public APDUResponseException(String message, int sw) {
        super(message);
        this.sw = sw;
    }

    public APDUResponseException(String message, ResponseAPDU responseAPDU) {
        this(message, responseAPDU.getSW());
    }

    public static void throwIfSwIsDifferentTo(int swWaited, int swResponse) {
        if (swWaited != swResponse)
            throw new APDUResponseException(
                    String.format("sw response waited: %sd, actual: %sd", swWaited, swResponse), swResponse
            );
    }

}
