package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CompactDate extends ByteDate {

    public static CompactDate now() {
        return new CompactDate(DateUtils.fromCompactLocalDate(LocalDate.now()));
    }

    public CompactDate(int code) {
        super(code);
        setDate(DateUtils.toCompactLocalDate(code));
    }

    public static byte[] toBytes(int dateCompact) {
        byte msb = (byte) ((dateCompact & 0xff00) >> 8);
        byte lsb = (byte) (dateCompact & 0x00ff);
        return new byte[]{msb, lsb};
    }

}
