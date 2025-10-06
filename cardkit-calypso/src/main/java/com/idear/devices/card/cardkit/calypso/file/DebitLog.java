package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.ReverseDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.LongDate;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.SvDebitLogRecord;

@EqualsAndHashCode(callSuper = true)
@Data
public class DebitLog extends File {
    private int amount;
    private int date;
    private int time;
    private int kvc;
    private String samId;
    private int samNum;
    private int balance;
    private int svtNum;

    public DebitLog(SvDebitLogRecord svDebitLogRecord) {

        if (svDebitLogRecord == null)
            throw new NullPointerException("debit record can not be null");

        this.amount = svDebitLogRecord.getAmount();
        this.date = ByteUtils.extractInt(svDebitLogRecord.getDebitTime(), 0, 2, false);
        this.time = ByteUtils.extractInt(svDebitLogRecord.getDebitTime(), 0, 2, false);
        this.samId = HexUtil.toHex(ByteUtils.extractInt(svDebitLogRecord.getSamId(), 0, 4, false));
        this.kvc = svDebitLogRecord.getKvc();
        this.samNum = svDebitLogRecord.getSamTNum();
        this.balance = svDebitLogRecord.getBalance();
        this.svtNum = svDebitLogRecord.getSvTNum();
    }

    public DebitLog(byte[] data) {
        super(HexUtil.toHex(data));

        this.amount = ByteUtils.extractInt(data, 0, 4, false);
        this.date = ByteUtils.extractInt(data, 4, 2, false);
        this.time = ByteUtils.extractInt(data, 6, 2, false);
        this.kvc = data[8] & 0xff;
        this.samId = HexUtil.toHex(ByteUtils.extractInt(data, 9, 4, false));
        this.balance = ByteUtils.extractInt(data, 13, 4, false);
        this.samNum = data[17] & 0xff;
    }

}
