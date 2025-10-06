package com.idear.devices.card.cardkit.core.io;

import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ByteDate {
    private LocalDate date;
    private int code;

    public ByteDate(int code) {
        this.code = code;
        this.date = DateUtils.toLocalDate(code);
    }
}
