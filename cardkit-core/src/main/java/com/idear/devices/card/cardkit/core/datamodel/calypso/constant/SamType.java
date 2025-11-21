package com.idear.devices.card.cardkit.core.datamodel.calypso.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SamType implements IDataModel {

    SPP(0x0A),
    SP(0x10),
    SL(0x30),
    DV(0x80),
    CPP(0x40),
    CPPC(0x48),
    CPB(0x51),
    CPS(0x52),
    CPT(0x53),
    CLB(0x61),
    CLS(0x62),
    CLT(0x63),
    CVB(0x71),
    CVS(0x72),
    CVT(0x73),
    RFU(-1);

    private final int value;

    public static SamType decode(int samType) {
        for (SamType v : values()) {
            if (v.value == samType) {
                return v;
            }
        }
        return RFU;
    }
}
