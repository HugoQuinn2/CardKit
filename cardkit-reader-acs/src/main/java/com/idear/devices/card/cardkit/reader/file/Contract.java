package com.idear.devices.card.cardkit.reader.file;

import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class Contract extends File {

    private int id;

    private int version;
    private ContractStatus status;
    private int rfu;
    private int startDate;
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
    private int saleDate;
    private int saleSam;
    private int authKvc;
    private int authenticator;

    public Contract(int id, byte[] data) {
        super(HexUtil.toHex(data));
        this.id = id;

        if (data == null)
            throw new IllegalArgumentException("Null data.");

        if (data.length > CDMX.RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        if (data.length < CDMX.RECORD_SIZE) {
            byte[] tmp = new byte[CDMX.RECORD_SIZE];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }

        this.version              = data[0] & 0xff;
        this.status               = ContractStatus.decode(data[1] & 0xff);
        this.rfu                  = (data[2] & 0b11000000) >> 6;
        this.startDate            = ByteUtils.extractInt(data, 2, 2, false) & 0x3fff;
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
        this.saleDate             = ByteUtils.extractInt(data, 16, 2, false);
        this.saleSam              = ByteUtils.extractInt(data, 18, 4, false);
        this.authKvc              = data[25] & 0xff;
        this.authenticator        = ByteUtils.extractInt(data, 26, 3, false);
    }
}