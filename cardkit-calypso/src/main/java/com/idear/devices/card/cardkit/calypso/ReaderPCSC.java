package com.idear.devices.card.cardkit.calypso;

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

/**
 * Represents a PC/SC-based Calypso card reader.
 * <p>
 * This class manages initialization, card detection, and session updates
 * for Calypso-compatible cards using the Keyple PCSC plugin.
 * It integrates SAM (Secure Access Module) handling and provides
 * event-driven card observation.
 * </p>
 *
 * <p>Dependencies:</p>
 * <ul>
 *   <li>{@link CalypsoExtensionService}</li>
 *   <li>{@link SmartCardService}</li>
 *   <li>{@link PcscPluginFactoryBuilder}</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ReaderPCSC reader = new ReaderPCSC(
 *                 "READER_NAME",
 *                 new CalypsoSam("SAM_NAME", "lockSecret")
 *         );
 *
 * reader.init();
 * reader.initCardObserver("aid");
 * }</pre>
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class ReaderPCSC extends Reader<CardReaderEvent> {

    /** The name of the physical card reader. */
    private final String readerName;
    /** The associated Calypso SAM instance used for secure transactions. */
    private final CalypsoSam calypsoSam;
    /** Manager responsible for Calypso secure regular transactions. */
    private SecureRegularModeTransactionManager cardTransactionManager;
    /** Manages card selection and filtering based on AIDs. */
    private CardSelectionManager cardSelectionManager;
    /** The underlying physical card reader instance. */
    private CardReader cardReader;
    /** Represents the currently selected Calypso card. */
    private CalypsoCard calypsoCard;

    // Start Pcsc plugin
    public static final SmartCardService smartCardService = SmartCardServiceProvider.getService();
    public static final Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Calypso card services required
    public static final ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();
    public static final CalypsoExtensionService calypsoExtensionService = CalypsoExtensionService.getInstance();
    public static final CalypsoCardApiFactory calypsoCardApiFactory = calypsoExtensionService.getCalypsoCardApiFactory();



    /**
     * Retrieves the set of available reader names registered in the PCSC plugin.
     *
     * @return A set containing all available reader names.
     */
    public static Set<String> getReadersName() {
        return plugin.getReaderNames();
    }

    /**
     * Initializes the reader by binding to the physical PCSC reader
     * and initializing the associated SAM.
     *
     * @throws Exception if the reader is not found or initialization fails.
     */
    @Override
    public void init() throws Exception {
        cardReader = Assert.isNull(plugin.getReader(readerName), "Reader '%s' not founded.", readerName);
        calypsoSam.init();
    }

    @Override
    public void disconnect() {

    }

    /**
     * Updates the active Calypso card session by selecting and retrieving
     * the currently inserted card.
     * <p>
     * If the card selection fails or no matching card is found,
     * the operation is safely aborted without throwing an exception.
     * </p>
     */
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

    /**
     * Initializes the card observer and detection process for a specific Calypso AID.
     * <p>
     * Sets up a {@link CardSelectionManager} to filter cards by the provided AID,
     * attaches observers for card events, and starts repeating detection mode.
     * </p>
     *
     * @param aid The Calypso application identifier (AID) to filter cards.
     */
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

    /**
     * Internal observer class for handling card reader events and observation errors.
     * <p>
     * Implements both {@link CardReaderObserverSpi} and
     * {@link CardReaderObservationExceptionHandlerSpi} to react to
     * detection events and handle runtime exceptions during observation.
     * </p>
     */
    private final class CardEventObserver implements
            CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

        /**
         * Called when an exception occurs during card reader observation.
         *
         * @param contextInfo Contextual information about the error.
         * @param readerName  The name of the reader that caused the error.
         * @param e           The exception thrown during observation.
         */
        @Override
        public void onReaderObservationError(
                String contextInfo, String readerName, Throwable e) {
            log.error("Reader '{}' error reading card: {}", readerName, e.getMessage());
        }

        /**
         * Called whenever a new {@link CardReaderEvent} occurs.
         * <p>
         * This method delegates the event to the parent reader's event dispatcher.
         * </p>
         *
         * @param e The event triggered by the card reader.
         */
        @Override
        public void onReaderEvent(CardReaderEvent e) {
            fireEvent(e);
        }
    }

}
