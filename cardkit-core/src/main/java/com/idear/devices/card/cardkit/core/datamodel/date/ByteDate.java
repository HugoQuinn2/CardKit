package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ByteDate extends Item implements IDataModel {

    private LocalDate date;
    private final int code;

    public ByteDate(int code) {
        this.code = code;
    }

    public boolean isValid() {
        if (date == null)
            return false;

        return !date.isBefore(LocalDate.now());
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
