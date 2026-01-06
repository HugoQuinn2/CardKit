import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.core.utils.Strings;
import com.idear.devices.card.cardkit.pcsc.PcscAbstractReader;
import com.idear.devices.card.cardkit.pcsc.transaction.FreeCalypsoTransactionManager;
import org.junit.jupiter.api.Test;

import javax.smartcardio.ResponseAPDU;

public class PcscTest {
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void terminals() throws Exception {
        PcscAbstractReader samReader = new PcscAbstractReader(PcscAbstractReader.ACS_SAM_READER);
        samReader.connect();
    }

    @Test
    public void unlockSam() throws Exception {
        PcscAbstractReader carReader = new PcscAbstractReader(PcscAbstractReader.ACS_CARD_READER);
        PcscAbstractReader samReader = new PcscAbstractReader(PcscAbstractReader.ACS_SAM_READER);

        carReader.connect();
        samReader.connect();

        samReader.connectToCard();
        carReader.connectToCard();

        FreeCalypsoTransactionManager fctm = new FreeCalypsoTransactionManager(carReader, samReader);
        fctm.setPrePersonalizationMode(FreeCalypsoTransactionManager.PrePersonalizationMode.LEGACY);

        ResponseAPDU unlockSamResponse = fctm.unlockSam(ByteUtils.hexToBytes(lockSecret));
        ResponseAPDU selectApplicationResponse = fctm.selectApplication(ByteUtils.toAsciiBytes("0ETP.ICA"));
        String serial = Strings.extractBetween(ByteUtils.toHex(selectApplicationResponse.getData()), "A516BF0C13C708", "5307");

        ResponseAPDU selectDiversifierResponse = fctm.selectDiversifier(ByteUtils.hexToBytes(serial));
        ResponseAPDU getChallengeResponse = fctm.getChallenge();

        fctm.giveRandom(getChallengeResponse.getData());
        ResponseAPDU generateKeyResponse = fctm.samGenerateKey((byte) 0x21, (byte) 0x7E, (byte) 0x21, (byte) 0x7E);
    }
}
