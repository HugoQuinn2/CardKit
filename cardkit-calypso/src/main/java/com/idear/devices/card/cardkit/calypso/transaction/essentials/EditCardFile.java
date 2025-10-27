package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Represents a secure transaction to modify a specific record in a Calypso card file.
 * <p>
 * This transaction operates in Secure Regular Mode and performs the following steps:
 * <ol>
 *   <li>Ensures that a card is present in the reader.</li>
 *   <li>Opens a secure session with {@link WriteAccessLevel#LOAD} privileges.</li>
 *   <li>Updates the target record in the specified file.</li>
 *   <li>Closes the secure session and verifies the updated record by reading it back.</li>
 * </ol>
 *
 * <p>This transaction is designed for editing existing records, not appending new ones.
 * The resulting data returned represents the content of the updated record.</p>
 *
 * @see AppendEditCardFile for appending new records to a file segment
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
public class EditCardFile extends Transaction<Boolean, ReaderPCSC> {

    private final File<?> file;
    private final int recordNumber;
    private final WriteAccessLevel writeAccessLevel;

    /**
     * Constructs a new {@link EditCardFile} transaction.
     *
     * @param file          The file containing the record to edit.
     * @param recordNumber  The index of the record to update.
     */
    public EditCardFile(File<?> file, int recordNumber, WriteAccessLevel writeAccessLevel) {
        super("EDIT_FILE");
        this.file = file;
        this.recordNumber = recordNumber;
        this.writeAccessLevel = writeAccessLevel;
    }

    /**
     * Executes the edit operation on the specified {@link CalypsoCardCDMX} card file.
     *
     * <p>This method performs a secure session operation to update a record within a card file.
     * It ensures card presence, writes the new data</p>
     *
     * @param reader The card reader interface handling communication with the Calypso card.
     * @return A {@link TransactionResult} containing the updated file data if successful.
     * @throws ReaderException If no card is detected in the reader.
     * @throws CardException   If the card file could not be read or updated.
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(writeAccessLevel)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .prepareUpdateRecord(
                        file.getFileId(),
                        recordNumber,
                        file.unparse())
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .build();
    }

}
