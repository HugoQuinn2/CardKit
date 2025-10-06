package com.idear.devices.card.cardkit.core.io.datamodel;

import com.idear.devices.card.cardkit.core.utils.DateUtils;

public class ReverseDate extends ByteDate {

    public ReverseDate(int code) {
        super(code);
        setDate(DateUtils.toReverseLocalDate(code));
    }

}
