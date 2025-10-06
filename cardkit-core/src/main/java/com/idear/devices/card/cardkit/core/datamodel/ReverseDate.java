package com.idear.devices.card.cardkit.core.datamodel;

import com.idear.devices.card.cardkit.core.datamodel.date.ByteDate;
import com.idear.devices.card.cardkit.core.utils.DateUtils;

public class ReverseDate extends ByteDate {

    public ReverseDate(int code) {
        super(code);
        setDate(DateUtils.toReverseLocalDate(code));
    }

}
