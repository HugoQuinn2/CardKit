package com.idear.devices.card.cardkit.core.datamodel.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Manufacturer implements IDataModel {
    NXP(0xA00003),
    THALES_GD(0xA00001),
    OBERTHUR(0xA00002),
    SAMSUNG(0xA00005),
    ST_MICRO(0xA00006),
    INFINEON(0xA00004),
    UNKNOWN(-1);

    private final int value;
}
