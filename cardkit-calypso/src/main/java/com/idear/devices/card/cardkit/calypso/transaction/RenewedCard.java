package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SimpleReadCard;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Represents a transaction that verifies whether a Calypso card contract is about to expire
 * within a given number of days and renews it if necessary.
 * <p>
 * This transaction checks the card’s validity against a defined offset (days in advance).
 * If the contract is still valid but within the renewal window, it updates the contract
 * duration and start date, logs the renewal event, and writes both updates to the card.
 * </p>
 *
 * <p><b>Flow:</b></p>
 * <ol>
 *   <li>Reads the card and verifies its presence.</li>
 *   <li>Checks if the contract is expired or within the renewal threshold (daysOffset).</li>
 *   <li>If renewal is required:
 *     <ul>
 *       <li>Updates contract duration and start date.</li>
 *       <li>Creates and appends a renewal event {@link TransactionType#SV_CONTRACT_RENEWAL} to the event file.</li>
 *       <li>Updates the contract file on the card.</li>
 *     </ul>
 *   </li>
 *   <li>Returns a successful transaction result, whether renewal was performed or not.</li>
 * </ol>
 *
 * <p>
 * If the card is not detected in the reader, a {@link CardException} is thrown.
 * </p>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0
 */
public class RenewedCard extends Transaction<Boolean, ReaderPCSC> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final int locationId;
    private final Contract contract;
    private final int daysOffset;

    private int duration = 60;

    /**
     * Creates a new {@code RenewedContract} transaction.
     *
     * @param calypsoCardCDMX the Calypso card wrapper
     * @param locationId      the location ID where the renewal occurs
     * @param contract        the contract to be verified and potentially renewed
     * @param daysOffset      the number of days before expiration to trigger a renewal
     */
    public RenewedCard(CalypsoCardCDMX calypsoCardCDMX, int locationId, Contract contract, int daysOffset) {
        super("renewed contract");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.locationId = locationId;
        this.contract = contract;
        this.daysOffset = daysOffset;
    }

    /**
     * Sets the duration in months of the renewed contract.
     *
     * @param duration the new duration value
     * @return this instance for method chaining
     */
    public RenewedCard duration(int duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Executes the renewal transaction.
     * <p>
     * This method reads the card, verifies contract validity, and renews it if within
     * the renewal window. It also saves an event record and updates the contract on the card.
     * </p>
     *
     * @param reader the Calypso card reader
     * @return a {@link TransactionResult} indicating whether the renewal was performed successfully
     * @throws CardException if no card is present in the reader
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        if (!reader.execute(new SimpleReadCard()).isOk())
            throw new CardException("no card on reader");

        if (!contract.isExpired(daysOffset))
            return TransactionResult
                    .<Boolean>builder()
                    .transactionStatus(TransactionStatus.OK)
                    .message("card contract does not require renewal expiration date: " + contract.getExpirationDate(daysOffset))
                    .data(true)
                    .build();

        // Renew duration and start date
        contract.setDuration(duration);
        contract.setStartDate(ReverseDate.now());

        // Save event on card and renew contract
        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.getCardTransactionManager()
                .prepareUpdateRecord(
                        contract.getFileId(),
                        1,
                        contract.unparse()
                );

        reader.execute(
                new SaveEvent(
                        TransactionType.SV_CONTRACT_RENEWAL,
                        calypsoCardCDMX.getEnvironment(),
                        contract,
                        0,
                        locationId,
                        0,
                        calypsoCardCDMX.getEvents().getNextTransactionNumber())
        );

        reader.getCardTransactionManager()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER);

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .message("card contract renewed, expiration date: " + contract.getExpirationDate(daysOffset))
                .data(true)
                .build();
    }

}
