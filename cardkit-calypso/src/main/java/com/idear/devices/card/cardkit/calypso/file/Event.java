package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.calypso.CalypsoSam;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Provider;
import com.idear.devices.card.cardkit.core.datamodel.date.RealTimeDate;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
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
    private int provider;
    private int locationId;
    private RealTimeDate dateTimeStamp;
    private int amount;
    private int firstServiceProvider = 0;
    private int firstLocationId = 0;
    private RealTimeDate firstDateTimeStamp;
    private int firstPassenger = 0;
    private int firstContractsUsed = 0;
    private int data = 0;

    public Event(int id) {
        super(null, CDMX.EVENT_FILE);
        this.id = id;
    }

    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(CDMX.RECORD_SIZE * 8);

        bit.setNextInteger(version, 8);
        bit.setNextInteger(transactionNumber, 24);
        bit.setNextInteger(transactionType.getValue(), 8);
        bit.setNextInteger(networkId, 8);
        bit.setNextInteger(provider, 8);
        bit.setNextInteger(locationId, 24);
        bit.setNextInteger(dateTimeStamp.getCode(), 32);
        bit.setNextInteger(amount, 24);
        bit.setNextInteger(firstServiceProvider, 8);
        bit.setNextInteger(firstLocationId, 24);
        bit.setNextInteger(firstDateTimeStamp.getCode(), 32);
        bit.setNextInteger(firstPassenger, 8);
        bit.setNextInteger(firstContractsUsed, 8);
        bit.setNextInteger(data, 16);

        setContent(HexUtil.toHex(bit.getData()));
        return bit.getData();
    }

    @Override
    public Event parse(byte[] data) {
        this.version              = data[0] & 0xff;
        this.transactionNumber    = ByteUtils.extractInt(data, 1, 3, false);
        this.transactionType      = TransactionType.decode(data[4] & 0xff);
        this.networkId            = data[5] & 0xff;
        this.provider             = data[6] & 0xff;
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

    public static Event builEvent(
            TransactionType transactionType,
            CalypsoSam calypsoSam,
            int transactionNumber,
            int locationId,
            int amount) {

        Event event = new Event(1);
        event.setTransactionType(transactionType);
        event.setTransactionNumber(transactionNumber);
        event.setLocationId(locationId);
        event.setAmount(amount);

        // sam data
        event.setProvider(calypsoSam.getSamProviderCode());
        event.setVersion(calypsoSam.getSamVersion());
        event.setNetworkId(calypsoSam.getSamVersion());

        event.setDateTimeStamp(RealTimeDate.now());
        event.setFirstDateTimeStamp(RealTimeDate.now());

        event.unparse();
        return event;
    }

}