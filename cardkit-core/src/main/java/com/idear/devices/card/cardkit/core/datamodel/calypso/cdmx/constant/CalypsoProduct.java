package com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CalypsoProduct implements IDataModel {
    CALYPSO_HCE(0),
    CALYPSO_PRIME(1),
    CALYPSO_LIGHT(2),
    CALYPSO_BASIC(3),
    RFU(-1);

    private final int value;

    public static CalypsoProduct decode(int value) {
        for (CalypsoProduct v : values()) {
            if (v.value ==  value)
                return v;
        }
        return RFU;
    }
}
