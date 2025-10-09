package com.idear.devices.card.cardkit.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public abstract class DateUtils {

    public static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;
    public static final LocalDate DAYS_UNTIL_1997_01_01 = LocalDate.of(1997, 1, 1);
    public static final long SECONDS_UNTIL_1997_01_01 = LocalDateTime.of(1997, 1, 1, 0, 0, 0).toEpochSecond(ZONE_OFFSET);

    public static LocalDate toCompactLocalDate(int dateCompact) {
        return DAYS_UNTIL_1997_01_01.plusDays(dateCompact);
    }

    public static LocalDate toLongLocalDate(int dateLong) {
        return DAYS_UNTIL_1997_01_01.plusDays(dateLong);
    }

    public static LocalDate toReverseLocalDate(int dateCompact) {
        int normalDateCompact = dateCompact ^ 0x3fff;
        return toCompactLocalDate(normalDateCompact);
    }

    public static LocalDateTime toRealTime(int date) {
        return LocalDateTime.ofEpochSecond(
                Integer.valueOf(date).longValue() + SECONDS_UNTIL_1997_01_01,
                0,
                ZONE_OFFSET);
    }

    public static LocalTime toCompactTime(int timeCompact) {
        int hours = timeCompact / 60;
        int minutes = timeCompact % 60;
        return LocalTime.of(hours, minutes);
    }

    /**
     * Converts a LocalDate to a compact integer representation (days since 1997-01-01).
     */
    public static int fromCompactLocalDate(LocalDate date) {
        return (int) ChronoUnit.DAYS.between(DAYS_UNTIL_1997_01_01, date);
    }

    /**
     * Converts a LocalDate to a long integer representation (same as compact).
     */
    public static int fromLongLocalDate(LocalDate date) {
        return fromCompactLocalDate(date);
    }

    /**
     * Converts a LocalDate to its reversed compact integer form (XOR with 0x3FFF).
     */
    public static int fromReverseLocalDate(LocalDate date) {
        int compact = fromCompactLocalDate(date);
        return compact ^ 0x3fff;
    }

    /**
     * Converts a LocalDateTime to an integer (seconds since 1997-01-01 00:00:00 UTC).
     */
    public static int fromRealTime(LocalDateTime dateTime) {
        long secondsSince1997 = dateTime.toEpochSecond(ZONE_OFFSET) - SECONDS_UNTIL_1997_01_01;
        return (int) secondsSince1997;
    }
}
