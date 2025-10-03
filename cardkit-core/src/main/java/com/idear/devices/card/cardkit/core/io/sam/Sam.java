package com.idear.devices.card.cardkit.core.io.sam;

import com.idear.devices.card.cardkit.core.exception.SamException;
import lombok.Data;

@Data
public abstract class Sam {
    public abstract void init() throws SamException;
}
