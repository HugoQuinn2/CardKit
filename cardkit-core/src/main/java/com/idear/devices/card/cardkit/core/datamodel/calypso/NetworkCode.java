package com.idear.devices.card.cardkit.core.datamodel.calypso;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum NetworkCode implements IDataModel {

    CDMX(0x01),
    RFU(-1);

    private final int value;

    NetworkCode(int value) {
        this.value = value;
    }

    public static NetworkCode decode(int value) {
        for (NetworkCode v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }
}
