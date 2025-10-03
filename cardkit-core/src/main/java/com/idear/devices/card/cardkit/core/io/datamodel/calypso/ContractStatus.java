package com.idear.devices.card.cardkit.core.io.datamodel.calypso;

import com.idear.devices.card.cardkit.core.io.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum ContractStatus implements IDataModel {

    CONTRACT_NEVER_USED(0x00),
    CONTRACT_PARTLY_USED(0x01),
    CONTRACT_TO_BE_RENEWED(0x03),
    CONTRACT_SUSPENDED(0x3F),
    CONTRACT_INVALID_AND_REFUNDED(0x7F),
    CONTRACT_ERASABLE(0xFF),
    RFU(-1);

    private final int value;

    ContractStatus(int value) {
        this.value = value;
    }

    public static ContractStatus decode(int value) {
        for (ContractStatus v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }

}
