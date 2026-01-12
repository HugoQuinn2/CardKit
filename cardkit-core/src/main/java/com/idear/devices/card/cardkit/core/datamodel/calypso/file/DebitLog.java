package com.idear.devices.card.cardkit.core.datamodel.calypso.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keypop.calypso.card.card.SvDebitLogRecord;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class DebitLog extends File<DebitLog> {

    private int amount;
    private CompactDate date;
    private CompactTime time;
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

        BitUtil bit = new BitUtil(Calypso.RECORD_SIZE * 8);

        bit.setNextInteger(amount, 16);
        bit.setNextInteger(date.getValue(), 16);
        bit.setNextInteger(time.getValue(), 16);
        bit.setNextInteger(kvc, 8);
        bit.setNextInteger((int) Long.parseLong(samId, 16), 32);
        bit.setNextInteger(samNum, 24);
        bit.setNextInteger(balance, 24);
        bit.setNextInteger(svtNum, 16);

        return bit.getData();
    }

    @Override
    public DebitLog parse(byte[] data) {

        this.amount = ByteUtils.extractInt(data, 0, 4, false);
        this.date = CompactDate.fromDays(ByteUtils.extractInt(data, 4, 2, false));
        this.time = CompactTime.fromMinutes(ByteUtils.extractInt(data, 6, 2, false));
        this.kvc = data[8] & 0xff;
        this.samId = String.format("%08X", ByteUtils.extractInt(data, 9, 4, false));
        this.balance = ByteUtils.extractInt(data, 13, 4, false);
        this.samNum = data[17] & 0xff;

        setContent(Strings.bytesToHex(data));
        return this;
    }

    public DebitLog parse(SvDebitLogRecord data) {
        amount = data.getAmount();
        date = CompactDate.fromDays(ByteUtils.extractInt(data.getDebitDate(), 0, 2, false));
        time = CompactTime.fromMinutes(ByteUtils.extractInt(data.getDebitTime(), 0, 2, false));
        kvc = data.getKvc() & 0xff;
        samId =  String.format("%08X", ByteUtils.extractInt(data.getSamId(), 0, 4, false));
        samNum = data.getSamTNum();
        balance = data.getBalance();
        svtNum = data.getSamTNum();

        setContent(Strings.bytesToHex(data.getRawData()));
        return this;
    }

    @JsonIgnore
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.of(date.getDate(), time.getTime());
    }
}
