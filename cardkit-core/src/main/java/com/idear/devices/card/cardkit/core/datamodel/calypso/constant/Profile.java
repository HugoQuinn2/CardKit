package com.idear.devices.card.cardkit.core.datamodel.calypso.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum Profile implements IDataModel {
    // -----------------------------
    // MULTIMODAL
    // -----------------------------
    GENERAL(0x000,
            0, 0, 0,
            120,
            0,
            PeriodType.encode(PeriodType.MONTH, 60),
            Tariff.STORED_VALUE,
            0,
            Collections.emptyList()
    ),

    DIF(0x300, 3, 0, 0, 60, 60, PeriodType.encode(PeriodType.MONTH, 60), Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 15, Arrays.asList(Equipment.VALIDATOR_WITH_SERVICE_DOOR, Equipment.SPECIAL_SERVICE_VALIDATOR)),
    COPACO(0x500, 0,0, 0, 36, 36, PeriodType.encode(PeriodType.MONTH, 60), Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 15, Arrays.asList(Equipment.VALIDATOR_WITH_SERVICE_DOOR, Equipment.SPECIAL_SERVICE_VALIDATOR)),
    CONTRALOR(0xD00, 0, 0, 0, 60, 60, PeriodType.encode(PeriodType.MONTH, 60), Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 15, Arrays.asList(Equipment.VALIDATOR_WITH_SERVICE_DOOR, Equipment.SPECIAL_SERVICE_VALIDATOR)),

    // -----------------------------
    // MONOMODAL
    // -----------------------------
    POLICE(0x600, 0, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    MAINTENANCE(0x700, 0, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    CASH_COLLECTION(0x800, 0, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    CARDS_SUPPLY(0xC00,12, 0, 0, 0, 0, 0, null, 0, Collections.emptyList()),
    SUPERVISOR(0x900,9, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    EMPLOYEE(0xA00, 0xA, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    VALUE_DEPOSIT(0xB00,0xB, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),


    ELDERLY_70(0x200, 2, 0, 0, 60, 0, 0, Tariff.FREE_PASS_WITH_SECURITY_BACKUP,0, Collections.emptyList()),
    ELDERLY_60(0xE00, 0xE, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITH_SECURITY_BACKUP,0, Collections.emptyList()),
    STUDENT(0x400, 0x4, 0, 0, 60, 6, PeriodType.encode(PeriodType.MONTH, 6), Tariff.FREE_PASS_WITH_SECURITY_BACKUP,0, Collections.emptyList()),
    RFU (-1, -1, -1, -1, -1, -1, -1, null,0, Collections.emptyList());

    private final int value;
    private final int prof1;
    private final int prof2;
    private final int prof3;
    private final int validityCard;
    private final int validityProfile;
    private final int validityContract;
    private final Tariff tariff;
    private final int passBack;
    private final List<Equipment> equipmentsExceptions;

    public static Profile decode(int prof1, int prof2, int prof3) {
        return Arrays.stream(values())
                .filter(p -> p.prof1 == prof1 && p.prof2 == prof2 && p.prof3 == prof3)
                .findFirst()
                .orElse(RFU);
    }

    public String getCode() {
        return String.format("%d%d%d", prof1, prof2, prof3);
    }

    public boolean isAllowedOn(Equipment equipment) {
        return !equipmentsExceptions.contains(equipment);
    }
}
