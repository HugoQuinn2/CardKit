package com.idear.devices.card.cardkit.calypso.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.ReverseDate;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class Contract extends File<Contract> {

    private int id;

    private int version;
    private ContractStatus status;
    private int rfu;
    private ReverseDate startDate;
    private int duration;
    private int network;
    private Provider provider;
    private Modality modality;
    private int counterCode;
    private Tariff tariff;
    private int journeyInterChanges;
    private int vehicleClassAllowed;
    private int restrictTime;
    private int restrictCode;
    private int periodJourney;
    private long location;
    private CompactDate saleDate;
    private int saleSam;
    private int saleCounter;
    private int authKvc;
    private int authenticator;

    public Contract(int id) {
        super(null, CDMX.CONTRACT_FILE);
        this.id = id;
    }

    public boolean isExpired(int daysOffset) {
        LocalDate expirationDate = startDate.getDate().plusMonths(duration).minusDays(daysOffset);
        return LocalDate.now().isAfter(expirationDate);
    }

    @JsonIgnore
    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(CDMX.RECORD_SIZE * 8);

        bit.setNextInteger(version, 8);
        bit.setNextInteger(status.getValue(), 8);
        bit.setNextInteger(rfu, 2);
        bit.setNextInteger(startDate.getValue(), 14);
        bit.setNextInteger(duration, 8);
        bit.setNextInteger(network, 8);
        bit.setNextInteger(provider.getValue(), 8);
        bit.setNextInteger(modality.getValue(), 1);
        bit.setNextInteger(counterCode, 2);
        bit.setNextInteger(tariff.getValue(), 5);
        bit.setNextInteger(journeyInterChanges, 1);
        bit.setNextInteger(vehicleClassAllowed, 2);
        bit.setNextInteger(restrictTime, 5);
        bit.setNextInteger(restrictCode, 8);
        bit.setNextInteger(periodJourney, 8);
        bit.setNextLong(location, 40);
        bit.setNextInteger(saleDate.getValue(), 16);
        bit.setNextInteger(saleSam, 32);
        bit.setNextInteger(saleCounter, 24);
        bit.setNextInteger(authKvc, 8);
        bit.setNextInteger(authenticator, 24);

        setContent(HexUtil.toHex(bit.getData()));
        return bit.getData();
    }

    @Override
    public Contract parse(byte[] data) {
        if (data == null)
            throw new IllegalArgumentException("Null data.");

        if (data.length > CDMX.RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        if (data.length < CDMX.RECORD_SIZE) {
            byte[] tmp = new byte[CDMX.RECORD_SIZE];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }

        setContent(HexUtil.toHex(data));

        this.version              = data[0] & 0xff;
        this.status               = ContractStatus.decode(data[1] & 0xff);
        this.rfu                  = (data[2] & 0b11000000) >> 6;
        this.startDate            = new ReverseDate(ByteUtils.extractInt(data, 2, 2, false) & 0x3fff);
        this.duration             = data[4] & 0xff;
        this.network              = data[5] & 0xff;
        this.provider             = Provider.decode(data[6] & 0xff);

        this.modality             = Modality.decode((data[7] & 0b10000000) >> 7);
        this.counterCode          = (data[7] & 0b01100000) >> 5;
        this.tariff               = Tariff.decode((data[7] & 0b00011111));

        this.journeyInterChanges  = (data[8] & 0b10000000) >> 7;
        this.vehicleClassAllowed  = (data[8] & 0b01100000) >> 5;
        this.restrictTime         = (data[8] & 0b00011111);

        this.restrictCode         = data[9] & 0xff;
        this.periodJourney        = data[10] & 0xff;
        this.location             = ByteUtils.extractLong(data, 11, 5, false);
        this.saleDate             = new CompactDate(ByteUtils.extractInt(data, 16, 2, false));
        this.saleSam              = ByteUtils.extractInt(data, 18, 4, false);
        this.saleCounter          = ByteUtils.extractInt(data, 22, 3, false);
        this.authKvc              = data[25] & 0xff;
        this.authenticator        = ByteUtils.extractInt(data, 26, 3, false);

        return this;
    }

}