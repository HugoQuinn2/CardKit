package com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum ContractStatus implements IDataModel {

    CONTRACT_NEVER_USED(0x00, true),
    CONTRACT_PARTLY_USED(0x01, true),
    CONTRACT_TO_BE_RENEWED(0x03, true),
    CONTRACT_SUSPENDED(0x3F, false),
    CONTRACT_INVALID_AND_REFUNDED(0x7F, false),
    CONTRACT_ERASABLE(0xFF, false),
    RFU(-1, false);

    private final int value;
    private final boolean accepted;

    ContractStatus(int value, boolean accepted) {
        this.value = value;
        this.accepted = accepted;
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
