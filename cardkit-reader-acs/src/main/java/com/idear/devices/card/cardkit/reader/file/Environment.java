package com.idear.devices.card.cardkit.reader.file;

import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class Environment extends File {

    private Version version;
    private Country country;
    private NetworkCode network;
    private int issuer;
    private int application;
    private int issuingDate;
    private int endDate;
    private int holderBirthDate;
    private int holderCompany;
    private int holderId;

    private Profile profile;

    private int prof1Date;
    private int prof2Date;
    private int prof3Date;

    private int holderPadding;

    public Environment(byte[] env) {
        super(HexUtil.toHex(env));

        if (env == null)
            throw new IllegalArgumentException("Null data.");

        if (env.length > CDMX.RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        if (env.length < CDMX.RECORD_SIZE) {
            byte[] tmp = new byte[CDMX.RECORD_SIZE];
            System.arraycopy(env, 0, tmp, 0, env.length);
            env = tmp;
        }

        this.version = Version.decode(ByteUtils.mostSignificantNibble(env[0]));
        this.country = Country.decode(ByteUtils.extractInt(env, 0, 2, false) & 0x0FFF);
        this.network = NetworkCode.decode(env[2] & 0xFF);
        this.issuer = env[3] & 0xFF;
        this.application = ByteUtils.extractInt(env, 4, 4, false);
        this.issuingDate = ByteUtils.extractInt(env, 8, 2, false);
        this.endDate = ByteUtils.extractInt(env, 10, 2, false);
        this.holderBirthDate = ByteUtils.extractInt(env, 12, 4, false);
        this.holderCompany = env[16] & 0xFF;
        this.holderId = ByteUtils.extractInt(env, 17, 4, false);

        this.profile = Profile.decode(
                ByteUtils.mostSignificantNibble(env[21]),
                ByteUtils.mostSignificantNibble(env[23]),
                ByteUtils.mostSignificantNibble(env[26])
        );

        this.prof1Date = ByteUtils.extractInt(
                ByteUtils.extractBytes(env, 21 * 8 + 4, 2), 0, 2, false);

        this.prof2Date = ByteUtils.extractInt(env, 24, 2, false);

        this.prof3Date = ByteUtils.extractInt(
                ByteUtils.extractBytes(env, 26 * 8 + 4, 2), 0, 2, false);

        this.holderPadding = ByteUtils.leastSignificantNibble(env[28]);
    }

}