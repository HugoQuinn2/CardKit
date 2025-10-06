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
    public static final long DAYS_UNTIL_1997_01_01 = LocalDate.of(1997, 1, 1).toEpochDay();
    public static final long SECONDS_UNTIL_1997_01_01 = LocalDateTime.of(1997, 1, 1, 0, 0, 0).toEpochSecond(ZONE_OFFSET);

    public static LocalDate toLocalDate(
            int dateCompact) {
        return LocalDate.ofEpochDay(toEpochDay(dateCompact));
    }

    public static String toHumanReadableLocalDateTime(
            int dateCompact) {
        return dateCompact == 0 ?
                "0" : toLocalDate(dateCompact).atStartOfDay().format(LOCAL_DATE_TIME_FORMAT);
    }

    public static String toHumanReadableLocalDate(
            int dateCompact) {
        return dateCompact == 0 ?
                "0" : toLocalDate(dateCompact).format(LOCAL_DATE_TIME);
    }

    public static int fromHumanReadableLocalDate(String localDate) {
        return fromLocalDate(
                LocalDate.parse(localDate, LOCAL_DATE_TIME));
    }

    public static int fromHumanReadableLocalDateTime(String localDateTime) {
        return fromLocalDate(LocalDateTime
                .parse(localDateTime, LOCAL_DATE_TIME_FORMAT)
                .toLocalDate());
    }

    public static int toDateReverse(int dateCompact) {
        return (dateCompact & 0x3fff) ^ 0x3fff;
    }

    public static byte[] toBytes(int dateCompact) {
        byte msb = (byte) ((dateCompact & 0xff00) >> 8);
        byte lsb = (byte) (dateCompact & 0x00ff);
        return new byte[]{msb, lsb};
    }

    public static int fromLocalDate(LocalDate localDate) {
        return (int) (localDate.toEpochDay() - DateUtils.DAYS_UNTIL_1997_01_01);
    }

    public static int fromLocalDateTime(LocalDateTime localDateTime) {
        return fromLocalDate(localDateTime.toLocalDate());
    }

    private static long toEpochDay(int dateCompact) {
        return Integer.valueOf(dateCompact).longValue() + DateUtils.DAYS_UNTIL_1997_01_01;
    }

}
