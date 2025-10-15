package com.idear.devices.card.cardkit.core.datamodel.location;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class LocationCode extends Item {
    private final int code;

    @JsonValue
    public String toJsonValue() {
        return Integer.toHexString(code);
    }

    @Override
    public String toString() {
        return toJsonValue();
    }

}
