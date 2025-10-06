package com.idear.devices.card.cardkit.reader;

import com.idear.devices.card.cardkit.core.io.reader.Reader;
import com.idear.devices.card.cardkit.core.utils.Assert;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.*;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.*;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.InvalidCardResponseException;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;

import java.util.Set;


@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class ReaderPCSC extends Reader<CardReaderEvent> {

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
    private CardSelectionManager cardSelectionManager;

    private CardReader cardReader;
    private LegacySam legacySam;
    private CalypsoCard calypsoCard;

    public static Set<String> getReadersName() {
        return plugin.getReaderNames();
    }

    @Override
    public void init() throws Exception {
        cardReader = Assert.isNull(plugin.getReader(readerName), "Reader '%s' not founded.", readerName);
        calypsoSam.init();
    }

    @Override
    public void disconnect() {

    }

    public void updateCalypsoCardSession() {
        SmartCard smartCard;

        try {
            smartCard = cardSelectionManager
                    .processCardSelectionScenario(cardReader)
                    .getActiveSmartCard();
        } catch (InvalidCardResponseException ex) {
            return;
        } catch (ReaderCommunicationException | CardCommunicationException ex) {
            return;
        }

        // Card selection didn't match
        if (smartCard == null) {
            return;
        }

        calypsoCard = (CalypsoCard) smartCard;
    }

    public void initCardObserver(String aid) {

        // Create a card selection manager
        cardSelectionManager = readerApiFactory.createCardSelectionManager();

        // Create a Calypso card selection
        CalypsoCardSelectionExtension calypsoCardSelection = calypsoCardApiFactory
                .createCalypsoCardSelectionExtension()
                .acceptInvalidatedCard();

        // Prepare the Calypso card selection
        cardSelectionManager.prepareSelection(
                readerApiFactory.createIsoCardSelector()
                        .filterByDfName(aid),
                calypsoCardSelection);

        // Schedule the Calypso card selection
        cardSelectionManager.scheduleCardSelectionScenario(
                (ObservableCardReader) cardReader,
                ObservableCardReader.NotificationMode.ALWAYS);

        CardEventObserver cardObserver = new CardEventObserver();
        ((ObservableCardReader) cardReader).setReaderObservationExceptionHandler(cardObserver);
        ((ObservableCardReader) cardReader).addObserver(cardObserver);
        ((ObservableCardReader) cardReader).startCardDetection(
                ObservableCardReader.DetectionMode.REPEATING);

        ObservableCardReader observableCardReader = (ObservableCardReader) cardReader;
        observableCardReader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING);
    }

    private final class CardEventObserver implements
            CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

        @Override
        public void onReaderObservationError(
                String contextInfo, String readerName, Throwable e) {
        }

        @Override
        public void onReaderEvent(CardReaderEvent e) {
            fireEvent(e);
        }
    }

}
