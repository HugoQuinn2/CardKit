package com.idear.devices.card.cardkit.core.datamodel.calypso.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Equipment implements IDataModel {

    TVM(0x00, false),
    TVM_EXTERNAL(0x01, false),
    PERSONALIZATION_TERMINAL(0x02, false),
    POS(0x03, false),
    VALIDATOR_CONSOLE(0x04, false),
    VALIDATOR_GENERAL(0x05, false),
    VALIDATOR_ON_BOARD(0x06, false),
    VALIDATOR_WITH_SERVICE_DOOR(0x07, true),
    PORTABLE_DEVICE(0x08, false),
    VALIDATOR_OUT(0x09, false),
    TVM_LIGHT(0x0A, false),
    SPECIAL_SERVICE_VALIDATOR(0x0B, true),
    ANY_VALIDATOR(0x0F, false),
    RFU(-1, false);

    private final int value;
    private final boolean specialService;

    public static Equipment decode(int value) {
        for (Equipment v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }
}
