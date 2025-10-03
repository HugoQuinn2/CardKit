package com.idear.devices.card.cardkit.core.io.datamodel.calypso;

import com.idear.devices.card.cardkit.core.io.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum Country implements IDataModel {

    MEXICO(0x484),
    RFU(-1);

    private final int value;

    Country(int value) {
        this.value = value;
    }

    public static Country decode(int value) {
        for (Country v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }
}
