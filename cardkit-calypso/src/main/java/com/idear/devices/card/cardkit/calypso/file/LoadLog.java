package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.SvLoadLogRecord;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoadLog extends File {
    private int date;
    private int free1;
    private int kvc;
    private int free2;
    private int balance;
    private int amount;
    private int time;
    private String samId;
    private int samTNum;
    private int svNum;

    public LoadLog(SvLoadLogRecord data) {
        super(null);

        if (data == null)
            throw new NullPointerException("Load log parse can not be null");

        this.date = ByteUtils.extractInt(data.getLoadDate(), 0, 2, false);
        this.free1 = data.getFreeData()[0] & 0xff;
        this.kvc = data.getKvc() & 0xff;
        this.free2 = data.getFreeData()[1] & 0xff;
        this.balance = data.getBalance();
        this.amount = data.getAmount();
        this.time = ByteUtils.extractInt(data.getLoadTime(), 0, 2, false);
        this.samId = HexUtil.toHex(ByteUtils.extractInt(data.getSamId(), 0, 4, false));
        this.samTNum = data.getSamTNum();
        this.svNum = data.getSvTNum();
    }

}
