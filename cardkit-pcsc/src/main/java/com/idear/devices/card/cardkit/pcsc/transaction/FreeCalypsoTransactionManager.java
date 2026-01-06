package com.idear.devices.card.cardkit.pcsc.transaction;

import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.pcsc.PcscAbstractReader;
import com.idear.devices.card.cardkit.pcsc.PcscUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

@RequiredArgsConstructor
@Getter
@Setter
public class FreeCalypsoTransactionManager {

    public static enum PrePersonalizationMode {
        REV_3_APP,
        LEGACY
    }

    private PrePersonalizationMode prePersonalizationMode = PrePersonalizationMode.LEGACY;

    // Commands for Application revision 3
    public static final String SAM_UNLOCK_APP = "8020 0000";
    public static final String CARD_GENERATE_KEY_APP = "8012 FF00 03 %s%s 90 20";
    public static final String SELECT_DIVERSIFIER_APP = "8014 0000";
    public static final String GIVE_RANDOM_APP = "8086 0000";

    // Commands for Legacy mod
    public static final String SAM_UNLOCK_LEGACY = "9420 0000";
    public static final String CARD_GENERATE_KEY_LEGACY = "9412 FF00 03 %s%s 00 20";
    public static final String SELECT_DIVERSIFIER_LEGACY = "9414 0000";
    public static final String GIVE_RANDOM_LEGACY = "9486 0000";

    public static final String CARD_SELECT_APPLICATION = "00A4 0400";
    public static final String GET_CHALLENGE = "0084 0000 08";
    public static final String CHANGE_KEY = "00D8 0001 20";

    private final PcscAbstractReader cardReader;
    private final PcscAbstractReader samReader;

    public ResponseAPDU unlockSam(byte... lockSecret) {
        return samReader.simpleCommand(
                PcscUtil.buildCommand(
                        prePersonalizationMode.equals(PrePersonalizationMode.LEGACY) ?
                                SAM_UNLOCK_LEGACY : SAM_UNLOCK_APP,
                        lockSecret
                )
        );
    }

    public ResponseAPDU selectSamKey(byte KIF, byte KVC) {
        return samReader.simpleCommand(new CommandAPDU(
                0x94, 0x84, 0x00, 0x00
        ));
    }

    public ResponseAPDU selectApplication(byte... aid) {
        return cardReader.simpleCommand(new CommandAPDU(
                0x00, 0xA4, 0x04, 0x00,
                aid
        ));
    }

    public ResponseAPDU selectDiversifier(byte... applicationSerialNumber) {
        return samReader.simpleCommand(
                PcscUtil.buildCommand(
                        prePersonalizationMode.equals(PrePersonalizationMode.LEGACY) ?
                                SELECT_DIVERSIFIER_LEGACY : SELECT_DIVERSIFIER_APP,
                        applicationSerialNumber
                )
        );
    }

    public ResponseAPDU getChallenge() {
        return cardReader.simpleCommand(new CommandAPDU(
                0x00, 0x84, 0x00, 0x00, 0x08
        ));
    }

    public ResponseAPDU giveRandom(byte... challengeCard) {
        return samReader.simpleCommand(
                PcscUtil.buildCommand(
                        prePersonalizationMode.equals(PrePersonalizationMode.LEGACY) ?
                                GIVE_RANDOM_LEGACY : GIVE_RANDOM_APP,
                        challengeCard
                )
        );
    }

    public ResponseAPDU samGenerateKey(byte cipheringKIF, byte cipheringKVC, byte transferKIF, byte transferKVC) {
        String command = prePersonalizationMode.equals(PrePersonalizationMode.LEGACY) ?
                CARD_GENERATE_KEY_LEGACY : CARD_GENERATE_KEY_APP;

        command = String.format(command,
                ByteUtils.toHex(cipheringKIF), ByteUtils.toHex(cipheringKVC));

        return samReader.simpleCommand(new CommandAPDU(
                ByteUtils.hexToBytes(command)
        ));
    }

}
