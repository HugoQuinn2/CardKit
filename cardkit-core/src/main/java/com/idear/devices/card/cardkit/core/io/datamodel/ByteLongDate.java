package com.idear.devices.card.cardkit.core.io.datamodel;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class ByteLongDate extends File implements IDataModel {

    private LocalDate date;
    private final int code;

    public ByteLongDate(int code) {
        this.code = code;
        this.date = DateUtils.toLongLocalDate(code);
    }

    /**
     * Returns the string representation of the date when converted to JSON.
     */
    @JsonValue
    public String toJsonValue() {
        return date.toString();
    }

    @Override
    public int getValue() {
        return this.code;
    }
}
