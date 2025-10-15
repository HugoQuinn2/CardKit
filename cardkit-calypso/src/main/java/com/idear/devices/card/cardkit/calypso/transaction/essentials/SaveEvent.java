package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.file.Environment;
import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.Getter;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

/**
 * Represents a Calypso transaction that saves an event record on a card.
 * <p>
 * The {@code SaveEvent} class builds an {@link Event} instance using transaction details
 * such as the type, environment, contract, passenger, and location.
 * It then optionally fires a card event and writes the record onto the card if
 * the transaction type requires it.
 * </p>
 *
 * <p>This class extends {@link Transaction} with a Boolean result type,
 * indicating success or failure of the operation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * SaveEvent saveEvent = new SaveEvent(
 *         TransactionType.DEBIT,
 *         environment,
 *         contract,
 *         passengerId,
 *         locationId,
 *         amount,
 *         transactionNumber);
 *
 * TransactionResult&lt;Boolean&gt; result = saveEvent.execute(reader);
 * </pre>
 *
 * @author Hugo
 * @since 1.0
 */
@Getter
public class SaveEvent extends Transaction<Boolean, ReaderPCSC> {

    /** The type of transaction being executed (e.g., RELOAD, GENERAL_DEBIT, etc.). */
    private final TransactionType transactionType;

    /** The sequential transaction number associated with this event. */
    private final int transactionNumber;

    /** The environment data used for event creation. */
    private final Environment environment;

    /** The contract file associated with the transaction. */
    private final Contract contract;

    /** The passenger identifier related to the event. */
    private final int passenger;

    /** The identifier of the location where the event occurs. */
    private final int locationId;

    /** The amount involved in the transaction. */
    private final int amount;

    /**
     * Constructs a new {@code SaveEvent} instance.
     *
     * @param transactionType  the type of the transaction
     * @param environment      the environment data used for the event
     * @param contract         the contract file related to the event
     * @param passenger        the passenger identifier
     * @param locationId       the ID of the location where the event occurs
     * @param amount           the transaction amount
     * @param transactionNumber the sequential number of this transaction
     */
    public SaveEvent(
            TransactionType transactionType,
            Environment environment,
            Contract contract,
            int passenger,
            int locationId,
            int amount,
            int transactionNumber) {
        super("save event");
        this.transactionType = transactionType;
        this.environment = environment;
        this.contract = contract;
        this.passenger = passenger;
        this.locationId = locationId;
        this.amount = amount;
        this.transactionNumber = transactionNumber;
    }

    /**
     * Executes the event-saving process using the provided {@link ReaderPCSC}.
     * <p>
     * The method performs the following steps:
     * <ul>
     *     <li>Verifies that a card is present in the reader.</li>
     *     <li>Builds an {@link Event} object with the given transaction data.</li>
     *     <li>Triggers a card event if the transaction type is reportable.</li>
     *     <li>If writable, opens a secure session and appends the event record to the card.</li>
     * </ul>
     * </p>
     *
     * @param reader the reader used to communicate with the Calypso card
     * @return a {@link TransactionResult} containing the operation status and result
     * @throws ReaderException if no card is detected in the reader
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        if (!reader.isCardOnReader())
            throw new ReaderException("no card on reader");

        // Build the event data
        Event event = Event.builEvent(
                transactionType,
                environment,
                contract,
                passenger,
                transactionNumber,
                locationId,
                amount
        );

        // Trigger the event on the reader if the transaction is reportable
        if (transactionType.isReported())
            reader.fireCardEvent(event);

        // Write the event to the card if required
        if (transactionType.isWritten()) {
            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareAppendRecord(event.getFileId(), event.unparse())
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.CLOSE_AFTER);
        }

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .build();
    }
}
