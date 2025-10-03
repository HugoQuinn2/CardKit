package com.idear.devices.card.cardkit.core.io.datamodel.calypso;

import com.idear.devices.card.cardkit.core.io.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum Modality implements IDataModel {

    MONOMODAL(0),
    MULTIMODAL(1),
    FORBIDDEN(-1);

    private final int value;

    Modality(int value) {
        this.value = value;
    }

    public static Modality decode(int value) {
        for (Modality v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return FORBIDDEN;
    }
}
