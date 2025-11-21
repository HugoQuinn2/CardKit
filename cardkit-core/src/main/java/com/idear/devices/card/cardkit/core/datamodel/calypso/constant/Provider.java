package com.idear.devices.card.cardkit.core.datamodel.calypso.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum Provider implements IDataModel {

    METROBUS_L1(0x01),
    METROBUS_L2(0x02),
    METROBUS_L3(0x03),
    METROBUS_L4(0x04),
    METROBUS_L5(0x05),
    METROBUS_L6(0x06),
    METROBUS_L7(0x07),
    TEST(0x14),
    ORT(0x15),
    METRO(0x32),
    CABLEBUS(0x3C),
    RTP(0x46),
    TREN_LIGERO(0x5A),
    TROLEBUS(0x5A),
    TIMT(0x5A),
    SEMOVI(0x64),
    ECOBICI(0x96),
    MEXIBUS(0xCC),
    RFU(-1);

    private final int value;

    Provider(int value) {
        this.value = value;
    }

    public static Provider decode(int value) {
        for (Provider v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }
}
