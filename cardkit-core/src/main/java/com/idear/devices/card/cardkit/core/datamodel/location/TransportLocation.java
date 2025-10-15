package com.idear.devices.card.cardkit.core.datamodel.location;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Equipment;
import lombok.Getter;

@Getter
public class TransportLocation extends LocationCode {

    public static String LOCATION_MATCH_CODE = "[0-9a-fA-F]{6,}";

    private final int transport;
    private final Equipment equipment;
    private final int deviceId;

    public TransportLocation(String code) {
        super(Integer.parseInt(code));

        if (!code.matches(LOCATION_MATCH_CODE))
            throw new IllegalArgumentException("Code can not be less than 6 characters and must be hexadecimal");

        this.transport    = Integer.parseInt(code.substring(0, 3));
        this.equipment  = Equipment.decode(Integer.parseInt(code.substring(3, 4)));
        this.deviceId   = Integer.parseInt(code.substring(4, 6));
    }

    public TransportLocation(int code) {
        super(code);
        if (code < 999999 && code > 0)
            throw new IllegalArgumentException("Code location can not be less than 999999");

        String sCode = Integer.toString(code);
        this.transport  = Integer.parseInt(sCode.substring(0, 3));
        this.equipment  = Equipment.decode(Integer.parseInt(sCode.substring(3, 4)));
        this.deviceId   = Integer.parseInt(sCode.substring(4, 6));
    }

}
