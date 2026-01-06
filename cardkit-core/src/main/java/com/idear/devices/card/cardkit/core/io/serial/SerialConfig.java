package com.idear.devices.card.cardkit.core.io.serial;

import lombok.Builder;
import lombok.Data;

/**
 * @author Victor Hugo Gaspar Quinn
 * @version 1.1.0
 */
@Data
@Builder
public class SerialConfig {
    private final String portName;

    @Builder.Default
    private final int baudRate = 9600;

    @Builder.Default
    private final int dataBits = 8;

    @Builder.Default
    private final int stopBits = 1;

    @Builder.Default
    private final int parity = 0;

    @Builder.Default
    private final long readIntervalDelay = 20;
}
