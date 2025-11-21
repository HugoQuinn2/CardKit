package com.idear.devices.card.cardkit.core.datamodel.calypso.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodType {
    MONTH(0b00),
    WEEK(0b01),
    DAY(0b10);

    private final int bits;

    public static PeriodType decode(int bits) {
        for (PeriodType v : values()) {
            if (v.bits == bits) {
                return v;
            }
        }

        throw new IllegalArgumentException("Invalid period bits: " + bits);
    }

    public static int encode(PeriodType period, int trips) {
        if (period == null)
            throw new IllegalArgumentException("Period cannot be null");

        if (trips < 0 || trips > 63)
            throw new IllegalArgumentException("Trips must be between 0 and 63");

        return (period.getBits() << 6) | (trips & 0b00111111);
    }
}
