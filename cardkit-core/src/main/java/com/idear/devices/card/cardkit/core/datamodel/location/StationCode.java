package com.idear.devices.card.cardkit.core.datamodel.location;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Equipment;
import lombok.Getter;

@Getter
public class StationCode extends LocationCode {

    public static String LOCATION_MATCH_CODE = "[0-9a-fA-F]{6,}";

    private final int line;
    private final int station;
    private final Equipment equipment;
    private final int deviceId;

    public StationCode(String code) {
        super(Integer.parseInt(code));

        if (!code.matches(LOCATION_MATCH_CODE))
            throw new IllegalArgumentException("Code can not be less than 6 characters and must be hexadecimal");

        this.line       = code.charAt(0) - '0';
        this.station    = Integer.parseInt(code.substring(1, 3));
        this.equipment  = Equipment.decode(Integer.parseInt(code.substring(3, 4)));
        this.deviceId   = Integer.parseInt(code.substring(4, 6));
    }

    public StationCode(int code) {
        super(code);
        if (code < 999999 && code > 0)
            throw new IllegalArgumentException("Code location can not be less than 999999");

        String sCode = Integer.toString(code);
        this.line       = sCode.charAt(0) - '0';
        this.station    = Integer.parseInt(sCode.substring(1, 3));
        this.equipment  = Equipment.decode(Integer.parseInt(sCode.substring(3, 4)));
        this.deviceId   = Integer.parseInt(sCode.substring(4, 6));
    }

    public StationCode(int line, int station, Equipment equipment, int deviceId) {
        super(
                Integer.parseInt(
                String.valueOf(line) +
                        String.valueOf(station) +
                        String.valueOf(equipment.getValue()) +
                        String.valueOf(deviceId)
        ));

        this.line = line;
        this.station = station;
        this.equipment = equipment;
        this.deviceId = deviceId;
    }

}
