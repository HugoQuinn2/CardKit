package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.date.DateTimeReal;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.io.card.cardProperty.CardProperty;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class Event extends File<Event> {

    private int id;

    private final CardProperty<Version> version = CardProperty.emptyProperty(Version.class);
    private int transactionNumber;
    private final CardProperty<TransactionType> transactionType = CardProperty.emptyProperty(TransactionType.class);
    private final CardProperty<NetworkCode> networkId = CardProperty.emptyProperty(NetworkCode.class);
    private final CardProperty<Provider> provider = CardProperty.emptyProperty(Provider.class);
    private final LocationCode locationId = LocationCode.emptyLocationCode();
    private DateTimeReal dateTimeStamp;
    private int amount;
    private final CardProperty<Provider> firstServiceProvider = CardProperty.emptyProperty(Provider.class);
    private final LocationCode firstLocationId = LocationCode.emptyLocationCode();
    private DateTimeReal firstDateTimeStamp;
    private int firstPassenger;
    private int firstContractsUsed;
    private int data = 0;

    public Event(int id) {
        super(null, Calypso.EVENT_FILE);
        this.id = id;
    }

    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(Calypso.RECORD_SIZE * 8);

        bit.setNextInteger(version.getValue(), 8);
        bit.setNextInteger(transactionNumber, 24);
        bit.setNextInteger(transactionType.getValue(), 8);
        bit.setNextInteger(networkId.getValue(), 8);
        bit.setNextInteger(provider.getValue(), 8);
        bit.setNextInteger(locationId.getValue(), 24);
        bit.setNextInteger(dateTimeStamp.getValue(), 32);
        bit.setNextInteger(amount, 24);
        bit.setNextInteger(firstServiceProvider.getValue(), 8);
        bit.setNextInteger(firstLocationId.getValue(), 24);
        bit.setNextInteger(firstDateTimeStamp.getValue(), 32);
        bit.setNextInteger(firstPassenger, 8);
        bit.setNextInteger(firstContractsUsed, 8);
        bit.setNextInteger(data, 16);

        setContent(HexUtil.toHex(bit.getData()));
        return bit.getData();
    }

    @Override
    public Event parse(byte[] data) {
        this.version.setValue(data[0] & 0xff);
        this.transactionNumber    = ByteUtils.extractInt(data, 1, 3, false);
        this.transactionType.setValue(data[4] & 0xff);
        this.networkId.setValue(data[5] & 0xff);
        this.provider.setValue(data[6] & 0xff);
        this.locationId.setValue(ByteUtils.extractInt(data, 7, 3, false));
        this.dateTimeStamp        = DateTimeReal.fromSeconds(ByteUtils.extractInt(data, 10, 4, false));
        this.amount               = ByteUtils.extractInt(data, 14, 3, false);
        this.firstServiceProvider.setValue(data[17] & 0xff);
        this.firstLocationId.setValue(ByteUtils.extractInt(data, 18, 3, false));
        this.firstDateTimeStamp   = DateTimeReal.fromSeconds(ByteUtils.extractInt(data, 21, 4, false));
        this.firstPassenger       = data[25] & 0xff;
        this.firstContractsUsed   = data[26] & 0xff;
        this.data                 = ByteUtils.extractInt(data, 27, 2, false);

        setContent(HexUtil.toHex(data));
        return this;
    }

    public static Event builEvent(
            int transactionType,
            int networkCode,
            int provider,
            int contractId,
            int passenger,
            int transactionNumber,
            int locationId,
            int amount) {
        Event event = new Event(1);

        event.getTransactionType().setValue(transactionType);
        event.getNetworkId().setValue(networkCode);

        event.setTransactionNumber(transactionNumber);
        event.getLocationId().setValue(locationId);
        event.setAmount(amount);

        event.getVersion().setValueByModel(Version.VERSION_3_3);
        event.getProvider().setValue(provider);
        event.setDateTimeStamp(DateTimeReal.now());
        event.setFirstDateTimeStamp(DateTimeReal.now());
        event.getFirstServiceProvider().setValue(provider);
        event.getFirstLocationId().setValue(locationId);
        event.setFirstContractsUsed(getContractBitmap(contractId));
        event.setFirstPassenger(passenger);

        event.unparse();
        return event;
    }

    /**
     * Returns the bitmap value for a single contract.
     * <p>
     * The LSB (bit 0) corresponds to contract #1, the MSB (bit 7) corresponds to contract #8.
     * </p>
     *
     * @param contractId contract number (1 to 8)
     * @return byte with only the corresponding bit set
     * @throws IllegalArgumentException if contractId is not between 1 and 8
     */
    public static byte getContractBitmap(int contractId) {
        if (contractId < 1 || contractId > 8) {
            throw new IllegalArgumentException("Contract ID must be between 1 and 8");
        }
        return (byte) (1 << (contractId - 1));
    }

}