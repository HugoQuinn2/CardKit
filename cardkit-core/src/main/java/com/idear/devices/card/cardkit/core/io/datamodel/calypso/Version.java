package com.idear.devices.card.cardkit.core.io.datamodel.calypso;

import com.idear.devices.card.cardkit.core.io.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum Version implements IDataModel {

    VERSION_3_3(0x01),
    RFU(-1);

    private final int value;

    Version(int value) {
        this.value = value;
    }

    public static Version decode(int value) {
        for (Version v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }

}
