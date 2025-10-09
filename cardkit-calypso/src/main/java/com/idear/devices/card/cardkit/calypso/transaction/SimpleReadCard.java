package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Transaction that performs a simple read of a Calypso card.
 * <p>
 * This transaction reads the card's serial number and balance.
 * It ensures that the DF is valid and initializes the secure session manager for further operations.
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class SimpleReadCard extends Transaction<CalypsoCardCDMX, ReaderPCSC> {

    /**
     * Creates a new transaction for reading a Calypso card.
     */
    public SimpleReadCard() {
        super("simple read card");
    }

    /**
     * Executes the simple read transaction.
     *
     * @param reader The card reader interface.
     * @return TransactionResult containing the read card data.
     * @throws CardException If the card is invalid or cannot be read.
     */
    @Override
    public TransactionResult<CalypsoCardCDMX> execute(ReaderPCSC reader) {
        // Initialize the card model to store the read data
        CalypsoCardCDMX calypsoCardCDMX = new CalypsoCardCDMX();

        // Ensure the reader session is up-to-date
        reader.updateCalypsoCardSession();
        CalypsoCard calypsoCard = reader.getCalypsoCard();

        // Check if the Dedicated File (DF) is invalid
        if (calypsoCard.isDfInvalidated()) {
            throw new CardException("Invalid DF status on card");
        }

        // Extract and set the card serial number
        calypsoCardCDMX.setSerial(HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));

        // Initialize the secure transaction manager for DEBIT operations
        SecureRegularModeTransactionManager cardTransactionManager =
                ReaderPCSC.calypsoCardApiFactory.createSecureRegularModeTransactionManager(
                        reader.getCardReader(),
                        calypsoCard,
                        reader.getCalypsoSam().getSymmetricCryptoSettingsRT()
                );
        reader.setCardTransactionManager(cardTransactionManager);

        // Perform a DEBIT SV get operation to update the balance
        cardTransactionManager
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        // Store the card balance
        calypsoCardCDMX.setBalance(calypsoCard.getSvBalance());

        // Return the result
        return TransactionResult.<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCardCDMX)
                .build();
    }
}
