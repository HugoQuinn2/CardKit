package com.idear.devices.card.cardkit.core.io.datamodel;

import com.idear.devices.card.cardkit.core.utils.DateUtils;

public class CompactDate extends ByteDate{

    public CompactDate(int code) {
        super(code);
        setDate(DateUtils.toCompactLocalDate(code));
    }

}
