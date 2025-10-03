package com.idear.devices.card.cardkit.core.io.datamodel.calypso;

import com.idear.devices.card.cardkit.core.io.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum Tariff implements IDataModel {

    FREE_PASS_WITH_SECURITY_BACKUP(0x06),
    FREE_PASS_WITHOUT_SECURITY_BACKUP(0x04),
    SEASON_PASS(0x03),
    TICKET_BOOK(0x02),
    STORED_VALUE(0x01),
    RFU(-1);

    private final int value;

    Tariff(int value) {
        this.value = value;
    }

    public static Tariff decode(int value) {
        for (Tariff v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }
}
