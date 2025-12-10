package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.datamodel.ValueDecoder;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.SamType;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.sam.Sam;
import com.idear.devices.card.cardkit.core.utils.Assert;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.generic.CardTransactionManager;
import org.eclipse.keyple.card.generic.ChannelControl;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.FreeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.card.transaction.spi.SymmetricCryptoCardTransactionManagerFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySamSelectionExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelector;

import java.util.Arrays;

@EqualsAndHashCode(callSuper = true)
@Data
public class KeypleCalypsoSam extends Sam {

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
    private FreeTransactionManager freeTransactionManager;

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
    public void init() throws SamException {

        if (samName == null || samName.isEmpty())
            throw new SamException("sam name reader can not be null or empty");

        if (lockSecret == null || lockSecret.isEmpty())
            throw new SamException("lock secret sam can not be null or empty");

        samReader = Assert.isNull(KeypleReader.plugin.getReader(samName), "sam reader '%s' not founded.");
        legacySam = initCalypsoSam(samReader, lockSecret);
        serial = HexUtil.toHex(legacySam.getSerialNumber());
    }

    public LegacySam initCalypsoSam(
            CardReader samReader,
            String lockSecret) throws SamException {

        // Get reader api factory
        ReaderApiFactory readerApiFactory = SmartCardServiceProvider.getService().getReaderApiFactory();

        // Create a SAM selection manager.
        CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

        // Create a card selector without filter
        CardSelector<BasicCardSelector> samCardSelector = readerApiFactory.createBasicCardSelector();

        // Get legacy sam api factory
        LegacySamApiFactory samApiFactory =
                LegacySamExtensionService.getInstance().getLegacySamApiFactory();

        // Create a SAM selection
        LegacySamSelectionExtension samSelection = samApiFactory
                .createLegacySamSelectionExtension()
                .prepareReadAllCountersStatus();

        // Set lock secret
        if (lockSecret != null)
            samSelection.setUnlockData(lockSecret);

        // Prepare SAM selection
        samSelectionManager.prepareSelection(samCardSelector, samSelection);

        // Get SAM
        LegacySam sam = null;
        try {
            sam = (LegacySam) samSelectionManager
                    .processCardSelectionScenario(samReader)
                    .getActiveSmartCard();
        } catch (Exception ex) {
            throw new SamException("Calypso SAM selection failed in reader %s.", samReader.getName());
        }

        if (sam == null)
            throw new SamException("No Calypso SAM in reader %s.", samReader.getName());

        genericSamTransactionManager = GenericExtensionService.getInstance()
                .createCardTransaction(samReader, sam);

        byte[] dataout = genericSamTransactionManager
                .prepareApdu("80BE00A030") // SAM Read Parameters APDU
                .processApdusToByteArrays(ChannelControl.KEEP_OPEN).get(0);

        byte[] parameters = Arrays.copyOfRange(dataout, 8, 37);
        this.parse(parameters);

        SymmetricCryptoCardTransactionManagerFactory symmetricCryptoCardTransactionManagerFactory =
                LegacySamExtensionService.getInstance().getLegacySamApiFactory()
                        .createSymmetricCryptoCardTransactionManagerFactory(
                                samReader, sam);

        // Card security settings for Transport app
        CalypsoCardApiFactory calypsoApiFactory = CalypsoExtensionService.getInstance()
                .getCalypsoCardApiFactory();
        symmetricCryptoSettingsRT = calypsoApiFactory.createSymmetricCryptoSecuritySetting(
                        symmetricCryptoCardTransactionManagerFactory)
                .enableSvLoadAndDebitLog()
                .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, (byte) 0x21)
                .assignDefaultKif(WriteAccessLevel.LOAD, (byte) 0x27)
                .assignDefaultKif(WriteAccessLevel.DEBIT, (byte) 0x30);

        freeTransactionManager = legacySamApiFactory.createFreeTransactionManager(samReader, sam);
        return sam;
    }

}
