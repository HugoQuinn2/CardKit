package com.idear.devices.card.cardkit.core.datamodel.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.idear.devices.card.cardkit.core.utils.ByteUtils.toHex;

@Getter
@AllArgsConstructor
public enum SmartCardType implements IDataModel {
    EMV(1),
    JAVACARD(2),
    SIM_USIM(3),
    CALYPSO(4),
    DESFIRE(5),
    EID_ICAO(6),
    GENERIC(7),
    UNKNOWN(-1);

    private int value;

    public static SmartCardType parseByAtr(byte[] atr, byte[] historical) {
        String hex = toHex(historical);

        if (toHex(atr).startsWith("3B9F") || toHex(atr).startsWith("3B9E"))
            return SmartCardType.SIM_USIM;

        if (hex.startsWith("70"))
            return SmartCardType.EMV;

        if (hex.startsWith("80318065"))
            return SmartCardType.CALYPSO;

        if (hex.startsWith("0031C0"))
            return SmartCardType.EID_ICAO;

        if (toHex(atr).startsWith("3B8") || hex.endsWith("98"))
            return SmartCardType.DESFIRE;

        if (hex.startsWith("6") || hex.startsWith("7"))
            return SmartCardType.JAVACARD;

        return SmartCardType.UNKNOWN;
    }
}
