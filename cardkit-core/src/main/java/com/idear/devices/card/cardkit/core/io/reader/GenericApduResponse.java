package com.idear.devices.card.cardkit.core.io.reader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GenericApduResponse {
    private final byte[] dataOut;
    private final String sw;
}
