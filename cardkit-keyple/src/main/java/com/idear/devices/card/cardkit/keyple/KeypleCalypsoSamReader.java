package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.datamodel.ValueDecoder;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.SamType;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.reader.AbstractReader;
import com.idear.devices.card.cardkit.core.io.reader.IBasicReader;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.generic.CardTransactionManager;
import org.eclipse.keyple.card.generic.ChannelControl;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.util.Arrays;

@EqualsAndHashCode(callSuper = true)
@Data
public class KeypleCalypsoSamReader extends AbstractReader implements IBasicReader {

    private final String samName;
    private final String lockSecret;
    private String serial;

    @ToString.Exclude
    private LegacySam legacySam;
    @ToString.Exclude
    private CardReader samReader;
    @ToString.Exclude
    private CardTransactionManager genericSamTransactionManager;
    @ToString.Exclude
    private SymmetricCryptoSecuritySetting symmetricCryptoSettingsRT;

    private boolean waitingForCardPresent;
    private boolean waitingForCardAbsent;

    // Sam data params
    public static final int RECORD_SIZE = 29;

    private int samEnableBits;
    private final ValueDecoder<SamType> samType = ValueDecoder.emptyDecoder(SamType.class);
    private int samNetworkReference;
    private int samVersion;
    private int samNetworkCode;
    private final ValueDecoder<Provider> samProviderCode = ValueDecoder.emptyDecoder(Provider.class);

    public static final LegacySamExtensionService legacySamExtensionService = LegacySamExtensionService.getInstance();
    private static final LegacySamApiFactory legacySamApiFactory = legacySamExtensionService.getLegacySamApiFactory();

    private void parse(byte [] data) {
        if (data == null)
            throw new IllegalArgumentException("Null data.");

        if (data.length > RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        byte[] tmp = new byte[RECORD_SIZE];
        if (data.length < RECORD_SIZE) {
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }

        setSamEnableBits(ByteUtils.extractInt(data, 0, 2, false));
        samType.setValue(data[0x0B] & 0xff);
        samProviderCode.setValue(data[0x0F] & 0xff);
        setSamNetworkReference(data[0x0C] & 0xff);
        setSamVersion(data[0x0D] & 0xff);
        setSamNetworkCode(data[0x0E] & 0xff);
    }

    @Override
    public void connect() throws Exception {
        samReader = KeypleUtil.getCardReaderMatchingName(samName, false);
        connectToCard();
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isCardOnReader() {
        return samReader != null && samReader.isCardPresent();
    }

    @Override
    public void connectToCard() {
        if (samReader == null)
            return;
        legacySam = KeypleUtil.selectAndUnlockSam(samReader, lockSecret);
        symmetricCryptoSettingsRT = KeypleUtil.startSymmetricSecuritySettings(samReader, legacySam);
        genericSamTransactionManager = GenericExtensionService.getInstance()
                .createCardTransaction(samReader, legacySam);

        serial = HexUtil.toHex(legacySam.getSerialNumber());
        ResponseAPDU dataout = simpleCommand(new CommandAPDU(0x80, 0xBE, 0x00, 0xA0, 0x30));

        byte[] parameters = Arrays.copyOfRange(dataout.getBytes(), 8, 37);
        this.parse(parameters);
    }

    @Override
    public void disconnectFromCard() {
        legacySam = null;
        serial = "";
        genericSamTransactionManager = null;
    }

    @Override
    public ResponseAPDU simpleCommand(CommandAPDU command) {
        if (genericSamTransactionManager == null)
            throw new SamException("connection to sam not started");

        ResponseAPDU responseAPDU = new ResponseAPDU(genericSamTransactionManager
                .prepareApdu(command.getBytes())
                .processApdusToByteArrays(ChannelControl.KEEP_OPEN).get(0));

        if (responseAPDU.getSW() != 0x9000)
            throw new RuntimeException("status response error " + responseAPDU.getSW());

        return responseAPDU;
    }
}
