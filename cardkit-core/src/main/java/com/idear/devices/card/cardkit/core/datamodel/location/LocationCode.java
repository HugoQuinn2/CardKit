package com.idear.devices.card.cardkit.core.datamodel.location;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Equipment;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class LocationCode extends Item implements IDataModel {

    private int value;

    @JsonValue
    public String toJsonValue() {
        return Integer.toHexString(value);
    }

    public Equipment getEquipment() {
        String sValue = Integer.toString(value);
        return Equipment.decode(Integer.parseInt(sValue.substring(3, 4)));
    }

    public int getDeviceId() {
        String sValue = Integer.toString(value);
        return Integer.parseInt(sValue.substring(4, 6));
    }

    public static LocationCode emptyLocationCode() {
        return new LocationCode(0);
    }

}
