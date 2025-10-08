package com.idear.devices.card.cardkit.core.datamodel;

import com.idear.devices.card.cardkit.core.datamodel.date.ByteDate;
import com.idear.devices.card.cardkit.core.utils.DateUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReverseDate extends ByteDate {

    public ReverseDate(int code) {
        super(code);
        setDate(DateUtils.toReverseLocalDate(code));
    }

    public int toInt() {
        return ReverseDate.toInt(getDate());
    }

    public static int toInt(LocalDate date) {
        long days = ChronoUnit.DAYS.between(DateUtils.DAYS_UNTIL_1997_01_01, date);
        return (int) (days ^ 0x3fff);
    }

    public static LocalDate toLocalDate(int reverseDate) {
        int normal = reverseDate ^ 0x3fff;
        return DateUtils.DAYS_UNTIL_1997_01_01.plusDays(normal);
    }


}
