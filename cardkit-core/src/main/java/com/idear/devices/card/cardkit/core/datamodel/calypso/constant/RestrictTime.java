package com.idear.devices.card.cardkit.core.datamodel.calypso.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestrictTime implements IDataModel {

    WITHOUT_RESTRICTION(0),
    NOT_VALID_MORNING(1),
    NOT_VALID_PEAK_HOURS(2),
    NOT_VALID_ON_WEEKDAYS(3),
    NOT_VALID_ON_WEEKENDS_AND_HOLIDAYS(4),
    VALID_ON_SCHOOL_DAYS(5),
    PASSBACK_15_MINUTES(6),
    RFU(-1);

    private final int value;

    public static RestrictTime decode(int value) {
        for (RestrictTime v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }

}
