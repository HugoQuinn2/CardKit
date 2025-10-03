package com.idear.devices.card.cardkit.reader;

import com.idear.devices.card.cardkit.core.io.reader.Reader;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.utils.Assert;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.generic.CardTransactionManager;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.*;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;


@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class ReaderAcs extends Reader {

    private final String readerName;
    private final CalypsoSam calypsoSam;

    // Start Pcsc plugin
    public static final SmartCardService smartCardService = SmartCardServiceProvider.getService();
    public static final Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Calypso card services required
    public static final ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();
    public static final CalypsoExtensionService calypsoExtensionService = CalypsoExtensionService.getInstance();
    public static final CalypsoCardApiFactory calypsoCardApiFactory = calypsoExtensionService.getCalypsoCardApiFactory();

    private SecureRegularModeTransactionManager cardTransactionManager;
    private CardTransactionManager samTransactionManager;
    private SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting;

    private CardReader cardReader;
    private LegacySam legacySam;
    private CalypsoCard calypsoCard;

    @Override
    public void connect() throws Exception {
        cardReader = Assert.isNull(plugin.getReader(readerName), "Reader '%s' not founded.", readerName);
        calypsoSam.init();
    }

    @Override
    public void disconnect() {

    }

    @Override
    public <T> TransactionResult<T> executeTransaction(Transaction<T> transaction) {
        return null;
    }


}
