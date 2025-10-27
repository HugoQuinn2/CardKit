package com.idear.devices.card.cardkit.calypso;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GenericApduResponse {
    private final byte[] dataOut;
    private final String sw;
}
