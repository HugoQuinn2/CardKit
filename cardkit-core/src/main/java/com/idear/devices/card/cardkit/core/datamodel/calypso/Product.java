package com.idear.devices.card.cardkit.core.datamodel.calypso;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Product implements IDataModel {
    CALYPSO_HCE(0),
    CALYPSO_PRIME(1),
    CALYPSO_LIGHT(2),
    CALYPSO_BASIC(3),
    RFU(-1);

    private final int value;

    public static Product decode(int value) {
        for (Product v : values()) {
            if (v.value ==  value)
                return v;
        }
        return RFU;
    }
}
