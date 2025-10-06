package com.idear.devices.card.cardkit.core.datamodel.date;

import com.idear.devices.card.cardkit.core.utils.DateUtils;

public class LongDate extends ByteDate{
    public LongDate(int code) {
        super(code);
        setDate(DateUtils.toLongLocalDate(code));
    }
}
