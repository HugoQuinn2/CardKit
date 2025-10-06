package com.idear.devices.card.cardkit.core.datamodel.calypso;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum AppSubType implements IDataModel {

    CDMX_RT(0xC0),
    RFU(-1);

    private final int value;

    AppSubType(int value) {
        this.value = value;
    }

    public static AppSubType decode(int value) {
        for (AppSubType v : values()) {
            if (v.value == value)
                return v;
        }
        return RFU;
    }

}
