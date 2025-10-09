package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Represents a transaction used to append or edit one or multiple records belonging
 * to the same segment on a Calypso card.
 * <p>
 * This transaction is typically used for data structures such as
 * {@code Events extends File<Events> implements List<Event>}, where multiple
 * entries must be written as part of a single logical file update.
 * </p>
 *
 * <p><b>Flow:</b></p>
 * <ol>
 *   <li>Ensures a valid Calypso card is present in the reader.</li>
 *   <li>Opens a secure session with {@link WriteAccessLevel#LOAD} permissions.</li>
 *   <li>Prepares a reload service operation (SV_RELOAD).</li>
 *   <li>Appends the new record or set of records using the serialized data from {@link File#unparse()}.</li>
 *   <li>Closes the secure session and sends the APDU commands to the card.</li>
 * </ol>
 *
 * <p>
 * If no card is detected, a {@link CardException} is thrown before any operation is performed.
 * </p>
 *
 * <p>
 * This transaction ensures atomicity within a single secure session: if one record fails,
 * the entire operation is rolled back by the card or the transaction manager.
 * </p>
 *
 * @see File
 * @see ReaderPCSC
 * @see SecureRegularModeTransactionManager
 * @see com.idear.devices.card.cardkit.calypso.transaction.RenewedContract
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
public class AppendEditCardFile extends Transaction<Boolean, ReaderPCSC> {

    private final File<?> file;

    /**
     * Creates a new append/edit transaction for the given
     * {@link com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX} card file.
     *
     * @param file the file instance containing the records or data segment to be written
     */
    public AppendEditCardFile(File<?> file) {
        super("append edit file");
        this.file = file;
    }

    /**
     * Executes the append/edit operation on the card.
     * <p>
     * This method opens a secure Calypso session, performs a service reload operation,
     * and appends one or more records using the provided file’s binary data.
     * </p>
     *
     * @param reader the Calypso card reader handling the communication
     * @return a {@link TransactionResult} indicating whether the transaction was successful
     * @throws CardException if no valid card is detected in the reader
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        if (!reader.execute(new SimpleReadCard()).isOk())
            throw new CardException("no card on reader");

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .prepareAppendRecord(
                        file.getFileId(),
                        file.unparse()
                )
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER);

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .build();
    }

}
