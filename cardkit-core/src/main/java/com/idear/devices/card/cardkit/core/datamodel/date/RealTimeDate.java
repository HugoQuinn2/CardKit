package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RealTimeDate extends Item implements IDataModel {
    private final int code;
    private LocalDateTime date;

    public RealTimeDate(int code) {
        this.code = code;
        setDate(DateUtils.toRealTime(code));
    }

    public boolean isValid() {
        if (date == null)
            return false;

        return !date.isBefore(LocalDateTime.now());
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
