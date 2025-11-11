package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import lombok.Getter;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

import java.util.SortedMap;

/**
 * Represents a transaction that performs a partial read of multiple records
 * from a specific file on a Calypso card.
 *
 * <p>This transaction allows reading only a portion of the records (from a starting
 * record to an ending record) and a defined number of bytes from each record,
 * starting at a specified offset. It is designed for efficient data retrieval
 * when full file reading is unnecessary.</p>
 *
 * <p>The operation uses a {@link SecureRegularModeTransactionManager} to prepare
 * and execute a secure set of read commands on the card. If the card is not present,
 * a {@link ReaderException} is thrown.</p>
 *
 * @see Transaction
 * @see ReaderPCSC
 * @see CalypsoCard
 * @see SimpleReadCard
 */
@Getter
public class ReadCardFilePartially extends Transaction<SortedMap<Integer, byte[]>, ReaderPCSC> {

    private final byte fileId;
    private final byte fromRecord;
    private final byte toRecord;
    private final int offset;
    private final int bytesToRead;

    /**
     * Constructs a new {@code ReadCardFilePartially} transaction.
     *
     * @param fileId The short file identifier (SFI) of the file to read.
     * @param fromRecord The first record index to read.
     * @param toRecord The last record index to read.
     * @param offset The starting byte offset within each record.
     * @param bytesToRead The number of bytes to read from each record.
     */
    public ReadCardFilePartially(byte fileId, byte fromRecord, byte toRecord, int offset, int bytesToRead) {
        super("READ_FILE_PARTIALLY");
        this.fileId = fileId;
        this.fromRecord = fromRecord;
        this.toRecord = toRecord;
        this.offset = offset;
        this.bytesToRead = bytesToRead;
    }

    /**
     * Executes the partial file read transaction using the specified {@link ReaderPCSC}.
     *
     * <p>The process includes:
     * <ul>
     *   <li>Executing a simple card read to ensure the card is present.</li>
     *   <li>Creating a secure regular mode transaction manager.</li>
     *   <li>Preparing and executing a partial read of multiple records.</li>
     *   <li>Collecting all read records and returning them in sorted order.</li>
     * </ul>
     * </p>
     *
     * @param reader The {@link ReaderPCSC} instance used to communicate with the card.
     * @return A {@link TransactionResult} containing a sorted map where the key is the record number
     *         and the value is the corresponding byte array of data.
     * @throws ReaderException If no card is detected in the reader.
     */
    @Override
    public TransactionResult<SortedMap<Integer, byte[]>> execute(ReaderPCSC reader) {
        if (!reader.getCardReader().isCardPresent()) {
            throw new ReaderException("No card on reader");
        }

        CalypsoCard calypsoCard = reader.getCalypsoCard();

        try {
            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareReadRecordsPartially(
                            fileId,
                            fromRecord,
                            toRecord,
                            offset,
                            bytesToRead)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);
        } catch (Exception e) {
            throw new CardException(e.getMessage());
        }

        SortedMap<Integer, byte[]> sortedMap = calypsoCard.getFileBySfi(fileId)
                .getData()
                .getAllRecordsContent();

        reader.setCalypsoCard(calypsoCard);
        return TransactionResult
                .<SortedMap<Integer, byte[]>>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(sortedMap)
                .build();
    }
}
