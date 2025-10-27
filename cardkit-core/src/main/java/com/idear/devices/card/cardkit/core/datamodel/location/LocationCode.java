package com.idear.devices.card.cardkit.core.datamodel.location;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Equipment;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LocationCode extends Item implements IDataModel {

    private final int value;

    @JsonValue
    public String toJsonValue() {
        return Integer.toHexString(value);
    }

    @Override
    public String toString() {
        return toJsonValue();
    }

    public Equipment getEquipment() {
        String sValue = Integer.toString(value);
        return Equipment.decode(Integer.parseInt(sValue.substring(3, 4)));
    }

    public int getDeviceId() {
        String sValue = Integer.toString(value);
        return Integer.parseInt(sValue.substring(4, 6));
    }

}
