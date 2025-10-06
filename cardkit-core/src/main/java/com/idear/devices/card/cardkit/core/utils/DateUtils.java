package com.idear.devices.card.cardkit.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public abstract class DateUtils {

    public static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;
    public static final LocalDate DAYS_UNTIL_1997_01_01 = LocalDate.of(1997, 1, 1);
    public static final LocalDate DAYS_UNTIL_1997_01_01_00_00_00 = LocalDate.of(1997, 1, 1);
    public static final long SECONDS_UNTIL_1997_01_01 = LocalDateTime.of(1997, 1, 1, 0, 0, 0).toEpochSecond(ZONE_OFFSET);

    public static LocalDate toCompactLocalDate(
            int dateCompact) {
        return DAYS_UNTIL_1997_01_01.plusDays(dateCompact);
    }

    public static LocalDate toLongLocalDate(int dateLong) {
        return DAYS_UNTIL_1997_01_01.plusDays(dateLong);
    }

    public static LocalDate toReverseLocalDate(int dateCompact) {
        int normalDateCompact = dateCompact ^ 0x3fff;
        return toCompactLocalDate(normalDateCompact);
    }

    public static byte[] toBytes(int dateCompact) {
        byte msb = (byte) ((dateCompact & 0xff00) >> 8);
        byte lsb = (byte) (dateCompact & 0x00ff);
        return new byte[]{msb, lsb};
    }


}
