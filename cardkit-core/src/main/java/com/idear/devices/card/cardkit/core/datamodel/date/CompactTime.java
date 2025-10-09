package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class CompactTime {

    private final LocalTime date;
    private final int code;

    public CompactTime(int code) {
        this.code = code;
        date = DateUtils.toCompactTime(code);
    }

    public static CompactTime now() {
        return new CompactTime(CompactTime.fromLocalDateTime(LocalDateTime.now()));
    }

    public static byte[] toBytes(int timeCompact) {
        byte msb = (byte) ((timeCompact & 0xff00) >> 8);
        byte lsb = (byte) (timeCompact & 0x00ff);
        return new byte[]{msb, lsb};
    }

    public static int fromLocalDateTime(LocalDateTime localDateTime) {
        LocalTime localTime = localDateTime.toLocalTime();
        return localTime.getHour() * 60 + localTime.getMinute();
    }

    @JsonValue
    public String toJsonValue() {
        return date.toString();
    }

}
