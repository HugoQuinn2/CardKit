package com.idear.devices.card.cardkit.core.datamodel.calypso;

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
    GENERAL(0x00, 0x00, 0x00, 120, 0, PeriodType.encode(PeriodType.MONTH, 60), Tariff.STORED_VALUE, 0, Collections.emptyList()),
    DIF(0x03, 0, 0, 60, 60, PeriodType.encode(PeriodType.MONTH, 60), Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 15, Arrays.asList(Equipment.VALIDATOR_WITH_SERVICE_DOOR, Equipment.SPECIAL_SERVICE_VALIDATOR)),
    COPACO(0x05, 0, 0, 36, 36, PeriodType.encode(PeriodType.MONTH, 60), Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 15, Arrays.asList(Equipment.VALIDATOR_WITH_SERVICE_DOOR, Equipment.SPECIAL_SERVICE_VALIDATOR)),
    CONTRALOR(0x0D, 0, 0, 60, 60, PeriodType.encode(PeriodType.MONTH, 60), Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 15, Arrays.asList(Equipment.VALIDATOR_WITH_SERVICE_DOOR, Equipment.SPECIAL_SERVICE_VALIDATOR)),

    // -----------------------------
    // MONOMODAL
    // -----------------------------
    POLICE(0x06, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    MAINTENANCE(0x07, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    CASH_COLLECTION(0x08, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    CARDS_SUPPLY(0x12, 0, 0, 0, 0, 0, null, 0, Collections.emptyList()),
    SUPERVISOR(0x09, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    EMPLOYEE(0x0A, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),
    VALUE_DEPOSIT(0x0B, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITHOUT_SECURITY_BACKUP, 0, Collections.emptyList()),


    ELDERLY_70(0x02, 0, 0, 60, 0, 0, Tariff.FREE_PASS_WITH_SECURITY_BACKUP,0, Collections.emptyList()),
    ELDERLY_60(0x0E, 0, 0, 120, 0, 0, Tariff.FREE_PASS_WITH_SECURITY_BACKUP,0, Collections.emptyList()),
    STUDENT(0x04, 0, 0, 60, 6, PeriodType.encode(PeriodType.MONTH, 6), Tariff.FREE_PASS_WITH_SECURITY_BACKUP,0, Collections.emptyList()),
    RFU (-1, -1, -1, -1, -1, -1, null,0, Collections.emptyList());

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

    @Override
    public int getValue() {
        return Integer.parseInt(getCode());
    }

    public boolean isAllowedOn(Equipment equipment) {
        return !equipmentsExceptions.contains(equipment);
    }
}
