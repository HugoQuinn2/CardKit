package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.apdu.ResponseApdu;
import com.idear.devices.card.cardkit.core.io.reader.AbstractReader;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.generic.CardTransactionManager;
import org.eclipse.keyple.card.generic.ChannelControl;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.reader.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

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
public class KeypleCardReader extends AbstractReader {

    private final String readerName;
    private final String aid;

    @ToString.Exclude
    private CardTransactionManager genericTransactionManager;
    @ToString.Exclude
    private CardReader cardReader;
    @ToString.Exclude
    private CalypsoCard calypsoCard;

    /**
     * Initializes the reader by binding to the physical PCSC reader
     * and initializing the associated SAM.
     *
     * @throws Exception if the reader is not found or initialization fails.
     */
    @Override
    public void connect() throws Exception {
        cardReader = KeypleUtil.getCardReaderMatchingName(readerName, true);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isCardOnReader() {
        if (cardReader == null)
            return false;

        return cardReader.isCardPresent();
    }

    /**
     * Updates the active Calypso card session by selecting and retrieving
     * the currently inserted card.
     * <p>
     * If the card selection fails or no matching card is found,
     * the operation is safely aborted without throwing an exception.
     * </p>
     */
    @Override
    public void connectToCard() {
        if (!isCardOnReader())
            throw new ReaderException("no card on reader");
        calypsoCard = KeypleUtil.selectCard(cardReader, aid);

        genericTransactionManager = GenericExtensionService.getInstance()
                .createCardTransaction(cardReader, calypsoCard);
    }

    @Override
    public void disconnectFromCard() {
        calypsoCard = null;
        genericTransactionManager = null;
    }

    @Override
    public ResponseApdu simpleCommand(CommandAPDU command) {
        if (genericTransactionManager  == null)
            throw new RuntimeException("connection to card not started");

        return new ResponseApdu(
                genericTransactionManager
                        .prepareApdu(command.getBytes())
                        .processApdusToByteArrays(ChannelControl.KEEP_OPEN).get(0));
    }
}
