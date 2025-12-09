package com.idear.devices.card.cardkit.core.datamodel.calypso.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Country;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.NetworkCode;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Profile;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Version;
import com.idear.devices.card.cardkit.core.datamodel.date.LongDate;
import com.idear.devices.card.cardkit.core.datamodel.ValueDecoder;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

import java.time.LocalDate;

/**
 * It contains general information about the transportation application and the cardholder.
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Environment extends File<Environment> {

    /** File identifier, required for its reading */
    public static final byte SFI_FILE = (byte) 0x07;

    /** Version of the Environment and Holder structure */
    private final ValueDecoder<Version> version = ValueDecoder.emptyDecoder(Version.class);

    /** Country identifier according to ISO 3166. */
    private final ValueDecoder<Country> country = ValueDecoder.emptyDecoder(Country.class);

    /** Transport network identifier.  */
    private final ValueDecoder<NetworkCode> network = ValueDecoder.emptyDecoder(NetworkCode.class);

    /** Identifier of the organization that acquired the card. */
    private int issuer;

    /** Alternative identifier to the application's serial number for transportation. */
    private int application;

    /** Start date of the transport card application */
    private CompactDate issuingDate;

    /** Expiration date of the card transport application */
    private CompactDate endDate;

    /** Cardholder's date of birth. */
    private LongDate holderBirthDate;

    /** Entity or Organization to which the cardholder belongs */
    private int holderCompany;

    /** Entity or Organization to which the cardholder belongs */
    private int holderId;

    /** Identifier of the profile associated with the cardholder */
    private final ValueDecoder<Profile> profile = ValueDecoder.emptyDecoder(Profile.class);

    /** Profile expiration date */
    private CompactDate prof1Date;

    /** Profile expiration date */
    private CompactDate prof2Date;

    /** Profile expiration date */
    private CompactDate prof3Date;

    /** Padding bits */
    private int holderPadding;

    /**
     * Create an empty {@link Environment} file with {@link Environment#SFI_FILE} file id
     */
    public Environment() {
        super(null, SFI_FILE);
    }

    /**
     * Verify if the {@link Environment#endDate} is not empty and is not expired
     *
     * @return {@code true} if {@link Environment#endDate} is not empty and is not expired,
     * otherwise return {@code false}
     */
    @JsonIgnore
    public boolean isApplicationExpired() {
        return !endDate.isEmpty() &&
                LocalDate.now().isAfter(endDate.getDate());
    }

    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(Calypso.RECORD_SIZE * 8);
        String profHex = String.format("%03X", profile.getValue());

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

        String prof1 = String.valueOf(profHex.charAt(0));
        String prof2 = String.valueOf(profHex.charAt(1));
        String prof3 = String.valueOf(profHex.charAt(2));

        bit.setNextInteger(Integer.parseInt(prof1, 16), 4);
        bit.setNextInteger(prof1Date.getValue(), 16);
        bit.setNextInteger(Integer.parseInt(prof2, 16), 4);
        bit.setNextInteger(prof2Date.getValue(), 16);
        bit.setNextInteger(Integer.parseInt(prof3, 16), 4);
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

        this.version.setValue(ByteUtils.mostSignificantNibble(data[0]));
        this.country.setValue(ByteUtils.extractInt(data, 0, 2, false) & 0x0FFF);
        this.network.setValue(data[2] & 0xFF);
        this.issuer = data[3] & 0xFF;
        this.application = ByteUtils.extractInt(data, 4, 4, false);

        this.issuingDate = CompactDate.fromDays(ByteUtils.extractInt(data, 8, 2, false));
        this.endDate = CompactDate.fromDays(ByteUtils.extractInt(data, 10, 2, false));
        this.holderBirthDate = LongDate.fromValue(ByteUtils.extractInt(data, 12, 4, false));

        this.holderCompany = data[16] & 0xFF;
        this.holderId = ByteUtils.extractInt(data, 17, 4, false);

        this.profile.setValue(Integer.parseInt(
                Integer.toHexString(ByteUtils.mostSignificantNibble(data[21])) +
                        Integer.toHexString(ByteUtils.mostSignificantNibble(data[23])) +
                        Integer.toHexString(ByteUtils.mostSignificantNibble(data[26])),
                16));

        this.prof1Date = CompactDate.fromDays(ByteUtils.extractInt(
                ByteUtils.extractBytes(data, 21 * 8 + 4, 2), 0, 2, false)) ;

        this.prof2Date = CompactDate.fromDays(ByteUtils.extractInt(data, 24, 2, false)) ;

        this.prof3Date = CompactDate.fromDays(ByteUtils.extractInt(
                ByteUtils.extractBytes(data, 26 * 8 + 4, 2), 0, 2, false));

        this.holderPadding = ByteUtils.leastSignificantNibble(data[28]);

        setContent(HexUtil.toHex(data));
        return this;
    }

    public static Environment buildEnvironment(
            int network,
            int profile) {
        Environment env = new Environment();

        env.getProfile().setValue(profile);
        env.getNetwork().setValue(network);

        env.getVersion().setValue(Version.VERSION_3_3);
        env.getCountry().setValue(Country.MEXICO);

        env.setIssuer(1);
        env.setApplication(0);

        env.setIssuingDate(CompactDate.now());
        env.setEndDate(CompactDate.fromLocalDate(LocalDate.now().plusYears(5)));
        env.setHolderBirthDate(LongDate.empty());

        env.setHolderCompany(0);
        env.setHolderId(0);

        env.setProf1Date(CompactDate.empty());
        env.setProf2Date(CompactDate.empty());
        env.setProf3Date(CompactDate.empty());

        return env;
    }

}