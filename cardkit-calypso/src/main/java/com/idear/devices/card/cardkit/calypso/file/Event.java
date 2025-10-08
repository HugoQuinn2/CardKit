package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.date.RealTimeDate;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class Event extends File<Event> {

    private int id;

    private int version;
    private int transactionNumber;
    private TransactionType transactionType;
    private int networkId;
    private int serviceProvider;
    private int locationId;
    private RealTimeDate dateTimeStamp;
    private int amount;
    private int firstServiceProvider;
    private int firstLocationId;
    private RealTimeDate firstDateTimeStamp;
    private int firstPassenger;
    private int firstContractsUsed;
    private int data;

    public Event(int id) {
        super(null, CDMX.EVENT_FILE);
        this.id = id;
    }

    @Override
    public byte[] unparse() {
        return new byte[0];
    }

    @Override
    public Event parse(byte[] data) {
        this.version              = data[0] & 0xff;
        this.transactionNumber    = ByteUtils.extractInt(data, 1, 3, false);
        this.transactionType      = TransactionType.decode(data[4] & 0xff);
        this.networkId            = data[5] & 0xff;
        this.serviceProvider      = data[6] & 0xff;
        this.locationId           = ByteUtils.extractInt(data, 7, 3, false);
        this.dateTimeStamp        = new RealTimeDate(ByteUtils.extractInt(data, 10, 4, false));
        this.amount               = ByteUtils.extractInt(data, 14, 3, false);
        this.firstServiceProvider = data[17] & 0xff;
        this.firstLocationId      = ByteUtils.extractInt(data, 18, 3, false);
        this.firstDateTimeStamp   = new RealTimeDate(ByteUtils.extractInt(data, 21, 4, false));
        this.firstPassenger       = data[25] & 0xff;
        this.firstContractsUsed   = data[26] & 0xff;
        this.data                 = ByteUtils.extractInt(data, 27, 2, false);

        setContent(HexUtil.toHex(data));
        return this;
    }
}