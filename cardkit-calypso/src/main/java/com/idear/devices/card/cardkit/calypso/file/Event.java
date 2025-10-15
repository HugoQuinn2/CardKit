package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.calypso.CalypsoSam;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.date.DateTimeReal;
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

    private Version version;
    private int transactionNumber;
    private TransactionType transactionType;
    private NetworkCode networkId;
    private Provider provider;
    private int locationId;
    private DateTimeReal dateTimeStamp;
    private int amount;
    private Provider firstServiceProvider;
    private int firstLocationId;
    private DateTimeReal firstDateTimeStamp;
    private int firstPassenger;
    private int firstContractsUsed;
    private int data = 0;

    public Event(int id) {
        super(null, CDMX.EVENT_FILE);
        this.id = id;
    }

    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(CDMX.RECORD_SIZE * 8);

        bit.setNextInteger(version.getValue(), 8);
        bit.setNextInteger(transactionNumber, 24);
        bit.setNextInteger(transactionType.getValue(), 8);
        bit.setNextInteger(networkId.getValue(), 8);
        bit.setNextInteger(provider.getValue(), 8);
        bit.setNextInteger(locationId, 24);
        bit.setNextInteger(dateTimeStamp.getValue(), 32);
        bit.setNextInteger(amount, 24);
        bit.setNextInteger(firstServiceProvider.getValue(), 8);
        bit.setNextInteger(firstLocationId, 24);
        bit.setNextInteger(firstDateTimeStamp.getValue(), 32);
        bit.setNextInteger(firstPassenger, 8);
        bit.setNextInteger(firstContractsUsed, 8);
        bit.setNextInteger(data, 16);

        setContent(HexUtil.toHex(bit.getData()));
        return bit.getData();
    }

    @Override
    public Event parse(byte[] data) {
        this.version              = Version.decode(data[0] & 0xff);
        this.transactionNumber    = ByteUtils.extractInt(data, 1, 3, false);
        this.transactionType      = TransactionType.decode(data[4] & 0xff);
        this.networkId            = NetworkCode.decode(data[5] & 0xff);
        this.provider             = Provider.decode(data[6] & 0xff);
        this.locationId           = ByteUtils.extractInt(data, 7, 3, false);
        this.dateTimeStamp        = DateTimeReal.fromSeconds(ByteUtils.extractInt(data, 10, 4, false));
        this.amount               = ByteUtils.extractInt(data, 14, 3, false);
        this.firstServiceProvider = Provider.decode(data[17] & 0xff);
        this.firstLocationId      = ByteUtils.extractInt(data, 18, 3, false);
        this.firstDateTimeStamp   = DateTimeReal.fromSeconds(ByteUtils.extractInt(data, 21, 4, false));
        this.firstPassenger       = data[25] & 0xff;
        this.firstContractsUsed   = data[26] & 0xff;
        this.data                 = ByteUtils.extractInt(data, 27, 2, false);

        setContent(HexUtil.toHex(data));
        return this;
    }

    public static Event builEvent(
            TransactionType transactionType,
            Environment environment,
            Contract contract,
            int passenger,
            int transactionNumber,
            int locationId,
            int amount) {

        Event event = new Event(1);
        event.setTransactionType(transactionType);
        event.setTransactionNumber(transactionNumber);
        event.setLocationId(locationId);
        event.setAmount(amount);
        event.setNetworkId(environment.getNetwork());
        event.setProvider(contract.getProvider());
        event.setDateTimeStamp(DateTimeReal.now());
        event.setFirstDateTimeStamp(DateTimeReal.now());
        event.setVersion(environment.getVersion());

        event.setFirstServiceProvider(contract.getProvider());
        event.setFirstLocationId(locationId);

        event.setFirstContractsUsed(getContractBitmap(contract.getId()));
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