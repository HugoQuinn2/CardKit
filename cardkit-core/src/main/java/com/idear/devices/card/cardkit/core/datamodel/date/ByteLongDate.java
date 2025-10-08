package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class ByteLongDate extends File<ByteLongDate> implements IDataModel {

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

    @Override
    public byte[] unparse() {
        return new byte[0];
    }

    @Override
    public ByteLongDate parse(byte[] data) {
        return null;
    }
}
