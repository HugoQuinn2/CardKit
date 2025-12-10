package com.idear.devices.card.cardkit.keyple.transaction.essentials;

import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import lombok.Getter;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.ElementaryFile;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;


/**
 * Represents a transaction that reads a specific file record from a Calypso card
 * using a secure session with the specified {@link WriteAccessLevel}.
 *
 * <p>This transaction establishes a secure channel with the card through the {@link KeypleReader}
 * and attempts to read the content of a record from an {@link ElementaryFile} identified by its SFI.
 * It performs a debit SV operation as part of the command flow.</p>
 *
 * <p>If the card is not detected or the file cannot be read, the transaction
 * will throw a {@link ReaderException} or {@link CardException}, respectively.</p>
 *
 * @see Transaction
 * @see KeypleReader
 * @see CalypsoCard
 * @see ElementaryFile
 * @see WriteAccessLevel
 *
 * @author Victor Hugo Gaspar Quinn
 */
@Getter
public class ReadCardFile extends Transaction<byte[], KeypleReader> {

    private final WriteAccessLevel writeAccessLevel;
    private final byte fileId;
    private final int record;

    /**
     * Constructs a new {@code ReadCardFile} transaction.
     *
     * @param writeAccessLevel The {@link WriteAccessLevel} used to open the secure session.
     * @param fileId The short file identifier (SFI) of the file to read.
     * @param record The record index to read from the file.
     */
    public ReadCardFile(WriteAccessLevel writeAccessLevel, byte fileId, int record) {
        super("READ_FILE");
        this.writeAccessLevel = writeAccessLevel;
        this.fileId = fileId;
        this.record = record;
    }

    /**
     * Executes the transaction using the specified {@link KeypleReader}.
     *
     * <p>The process includes:
     * <ul>
     *   <li>Performing a simple card read to ensure the card is present.</li>
     *   <li>Opening a secure regular mode session with the card.</li>
     *   <li>Preparing and executing a read record command on the target file.</li>
     *   <li>Performing an SV debit operation (as part of secure flow).</li>
     * </ul>
     * If successful, the method returns the file data as a byte array.</p>
     *
     * @param reader The {@link KeypleReader} instance used to communicate with the card.
     * @return A {@link TransactionResult} containing the file data and transaction status.
     * @throws ReaderException If no card is detected in the reader.
     * @throws CardException If the specified file cannot be read.
     */
    @Override
    public TransactionResult<byte[]> execute(KeypleReader reader) {
        CalypsoCard calypsoCard = reader.getCalypsoCard();

        try {
            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(writeAccessLevel)
                    .prepareReadRecord(fileId, record)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);
        } catch (Exception e) {
            throw new CardException(e.getMessage());
        }

        ElementaryFile elementaryFile = calypsoCard.getFileBySfi(fileId);

        if (elementaryFile == null)
            throw new CardException("The file %s could not be read", fileId);

        reader.setCalypsoCard(calypsoCard);
        return TransactionResult
                .<byte[]>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(elementaryFile.getData().getContent())
                .build();
    }

}
