package com.idear.devices.card.cardkit.core.datamodel.calypso;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;

public enum Profile implements IDataModel {

    // Multimodal
    GENERAL(0, 0, 0),
    DIF(3, 0, 0),
    COPACO(5, 0, 0),
    CONTRALOR(0xD, 0, 0),

    // Monomodal
    POLICE(6, 0, 0),
    MAINTENANCE(7, 0, 0),
    CASH_COLLECTION(8, 0, 0),
    CARDS_SUPPLY(12, 0, 0),
    SUPERVISOR(9, 0, 0),
    EMPLOYEE(0xA, 0, 0),
    VALUE_DEPOSIT(0xB, 0, 0),

    // TBD
    ELDERLY_70(2, 0, 0),
    ELDERLY_60(0xE, 0, 0),
    STUDENT(4, 0, 0),
    RFU (-1, -1, -1);


    private final int prof1;
    private final int prof2;
    private final int prof3;

    Profile(int prof1, int prof2, int prof3) {
        this.prof1 = prof1;
        this.prof2 = prof2;
        this.prof3 = prof3;
    }

    public static Profile decode(int prof1, int prof2, int prof3) {
        for (Profile v : values()) {
            if (v.prof1 == prof1 &&
                    v.prof2 == prof2 &&
                    v.prof3 == prof3) {
                return v;
            }
        }
        return RFU;
    }

    /**
     * Returns the code as a string concatenation of prof1, prof2 and prof3.
     * Example: DIF(3,0,0) -> "300"
     */
    public String getCode() {
        return String.format("%d%d%d", this.prof1, this.prof2, this.prof3);
    }

    /**
     * Returns the numeric value formed by concatenating prof1, prof2 and prof3.
     * Example: DIF(3,0,0) -> 300
     */
    @Override
    public int getValue() {
        return Integer.parseInt(getCode());
    }
}
