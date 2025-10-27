package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.date.LongDate;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class Environment extends File<Environment> {

    private Version version;
    private Country country;
    private NetworkCode network;
    private int issuer;
    private int application;
    private CompactDate issuingDate;
    private CompactDate endDate;
    private LongDate holderBirthDate;
    private int holderCompany;
    private int holderId;

    private Profile profile;

    private CompactDate prof1Date;
    private CompactDate prof2Date;
    private CompactDate prof3Date;

    private int holderPadding;

    public Environment() {
        super(null, Calypso.ENVIRONMENT_FILE);
    }

    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(Calypso.RECORD_SIZE * 8);

        bit.setNextInteger(version.getValue(), 4);
        bit.setNextInteger(country.getValue(), 12);
        bit.setNextInteger(network.getValue(), 8);
        bit.setNextInteger(issuer, 8);
        bit.setNextInteger(application, 32);
        bit.setNextInteger(issuingDate.getValue(), 16);
        bit.setNextInteger(endDate.getValue(), 16);
        bit.setNextInteger(holderBirthDate.getValue(), 32);
        bit.setNextInteger(holderCompany, 8);
        bit.setNextInteger(holderId, 32);

        bit.setNextInteger(profile.getProf1(), 4);
        bit.setNextInteger(prof1Date.getValue(), 16);
        bit.setNextInteger(profile.getProf2(), 4);
        bit.setNextInteger(prof2Date.getValue(), 16);
        bit.setNextInteger(profile.getProf3(), 4);
        bit.setNextInteger(prof3Date.getValue(), 16);
        bit.setNextInteger(holderPadding, 4);

        return bit.getData();
    }

    @Override
    public Environment parse(byte[] data) {

        if (data == null)
            throw new IllegalArgumentException("Null data.");

        if (data.length > Calypso.RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        if (data.length < Calypso.RECORD_SIZE) {
            byte[] tmp = new byte[Calypso.RECORD_SIZE];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }

        this.version = Version.decode(ByteUtils.mostSignificantNibble(data[0]));
        this.country = Country.decode(ByteUtils.extractInt(data, 0, 2, false) & 0x0FFF);
        this.network = NetworkCode.decode(data[2] & 0xFF);
        this.issuer = data[3] & 0xFF;
        this.application = ByteUtils.extractInt(data, 4, 4, false);

        this.issuingDate = CompactDate.fromDays(ByteUtils.extractInt(data, 8, 2, false));
        this.endDate = CompactDate.fromDays(ByteUtils.extractInt(data, 10, 2, false));
        this.holderBirthDate = LongDate.fromValue(ByteUtils.extractInt(data, 12, 4, false));

        this.holderCompany = data[16] & 0xFF;
        this.holderId = ByteUtils.extractInt(data, 17, 4, false);

        this.profile = Profile.decode(
                ByteUtils.mostSignificantNibble(data[21]),
                ByteUtils.mostSignificantNibble(data[23]),
                ByteUtils.mostSignificantNibble(data[26])
        );

        this.prof1Date = CompactDate.fromDays(ByteUtils.extractInt(
                ByteUtils.extractBytes(data, 21 * 8 + 4, 2), 0, 2, false)) ;

        this.prof2Date = CompactDate.fromDays(ByteUtils.extractInt(data, 24, 2, false)) ;

        this.prof3Date = CompactDate.fromDays(ByteUtils.extractInt(
                ByteUtils.extractBytes(data, 26 * 8 + 4, 2), 0, 2, false));

        this.holderPadding = ByteUtils.leastSignificantNibble(data[28]);

        setContent(HexUtil.toHex(data));
        return this;
    }
}