package com.idear.devices.card.cardkit.core.datamodel.constant;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SignalType implements IDataModel {
    DIRECT(0x3B),
    INVERSE(0x3f);

    private final int value;
}
