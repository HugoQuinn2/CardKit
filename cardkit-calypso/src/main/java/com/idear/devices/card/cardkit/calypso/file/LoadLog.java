package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.SvLoadLogRecord;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoadLog extends File<LoadLog> {

    private CompactDate date;
    private int free1;
    private int kvc;
    private int free2;
    private int balance;
    private int amount;
    private CompactTime time;
    private String samId;
    private int samTNum;
    private int svNum;

    public LoadLog() {
        super(null,  (byte) 0x00);
    }

    @Override
    public byte[] unparse() {
        return new byte[0];
    }

    @Override
    public LoadLog parse(byte[] data) {

        if (data == null)
            throw new IllegalArgumentException("Null data.");

        if (data.length > CDMX.RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        if (data.length < CDMX.RECORD_SIZE) {
            byte[] tmp = new byte[CDMX.RECORD_SIZE];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }

        date =  new CompactDate(ByteUtils.extractInt(data, 0, 2, false));
        free1 = data[2] & 0xff;
        kvc = data[3] & 0xff;
        free2 = data[4] & 0xff;
        balance = ByteUtils.extractInt(data, 64, 3, true);
        amount = ByteUtils.extractInt(data, 88, 3, true);
        time = new CompactTime(ByteUtils.extractInt(data, 104, 2, false));
        samId =  HexUtil.toHex(ByteUtils.extractInt(data, 136, 4, false));
        samTNum = ByteUtils.extractInt(data, 160, 3, false);
        svNum = ByteUtils.extractInt(data, 176, 2, false);

        setContent(HexUtil.toHex(data));
        return this;
    }

    public LoadLog parse(SvLoadLogRecord data) {
        if (data == null)
            throw new IllegalArgumentException("Null data.");

        date = new CompactDate(ByteUtils.extractInt(data.getLoadDate(), 0, 2, false));
        free1 = data.getFreeData()[0] & 0xff;
        kvc = data.getKvc() & 0xff;
        free2 = data.getFreeData()[1] & 0xff;
        balance = data.getBalance();
        amount = data.getAmount();
        time = new CompactTime(ByteUtils.extractInt(data.getLoadTime(), 0, 2, false));
        samId = HexUtil.toHex(ByteUtils.extractInt(data.getSamId(), 0, 4, false));
        samTNum = data.getSamTNum();
        svNum = data.getSvTNum();

        setContent(HexUtil.toHex(data.getRawData()));
        return this;
    }

}
