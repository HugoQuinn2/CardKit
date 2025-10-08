package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class DebitLog extends File<DebitLog> {

    private int amount;
    private int date;
    private int time;
    private int kvc;
    private String samId;
    private int samNum;
    private int balance;
    private int svtNum;

    public DebitLog() {
        super(null, (byte) 0x00);
    }

    @Override
    public byte[] unparse() {

        BitUtil bit = new BitUtil(CDMX.RECORD_SIZE * 8);

        bit.setNextInteger(amount, 16);
        bit.setNextInteger(date, 16);
        bit.setNextInteger(time, 16);
        bit.setNextInteger(kvc, 8);
        bit.setNextInteger(HexUtil.toInt(samId), 32);
        bit.setNextInteger(samNum, 24);
        bit.setNextInteger(balance, 24);
        bit.setNextInteger(svtNum, 16);

        return bit.getData();
    }

    @Override
    public DebitLog parse(byte[] data) {

        this.amount = ByteUtils.extractInt(data, 0, 4, false);
        this.date = ByteUtils.extractInt(data, 4, 2, false);
        this.time = ByteUtils.extractInt(data, 6, 2, false);
        this.kvc = data[8] & 0xff;
        this.samId = HexUtil.toHex(ByteUtils.extractInt(data, 9, 4, false));
        this.balance = ByteUtils.extractInt(data, 13, 4, false);
        this.samNum = data[17] & 0xff;

        setContent(HexUtil.toHex(data));
        return this;
    }
}
