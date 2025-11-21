package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.EditCardFile;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;

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

    public static final String NAME = "RENEWED_CARD";

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final NetworkCode networkCode;
    private final Provider provider;
    private final LocationCode locationId;
    private final Contract contract;
    private final int daysOffset;
    private final int duration;

    /**
     * Creates a new {@code RenewedContract} transaction.
     *
     * @param calypsoCardCDMX the Calypso card wrapper
     * @param locationId      the location ID where the renewal occurs
     * @param contract        the contract to be verified and potentially renewed
     * @param daysOffset      the number of days before expiration to trigger a renewal
     */
    public RenewedCard(
            CalypsoCardCDMX calypsoCardCDMX,
            NetworkCode networkCode,
            Provider provider,
            LocationCode locationId,
            Contract contract,
            int daysOffset,
            int duration) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.networkCode = networkCode;
        this.provider = provider;
        this.locationId = locationId;
        this.contract = contract;
        this.daysOffset = daysOffset;
        this.duration = duration;
    }

    /**
     * Creates a new {@code RenewedContract} transaction.
     *
     * @param calypsoCardCDMX the Calypso card wrapper
     * @param locationId      the location ID where the renewal occurs
     * @param contract        the contract to be verified and potentially renewed
     * @param daysOffset      the number of days before expiration to trigger a renewal
     */
    public RenewedCard(
            CalypsoCardCDMX calypsoCardCDMX,
            NetworkCode networkCode,
            Provider provider,
            LocationCode locationId,
            Contract contract,
            int daysOffset) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.networkCode = networkCode;
        this.provider = provider;
        this.locationId = locationId;
        this.contract = contract;
        this.daysOffset = daysOffset;
        this.duration = PeriodType.encode(PeriodType.MONTH, 60);
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
        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("card invalidated");

        if (!contract.isExpired(daysOffset))
            throw new CardException("card contract does not require renewal expiration date: " + contract.getExpirationDate(daysOffset));

        // Renew duration and start date
        contract.setDuration(duration);
        contract.setStartDate(ReverseDate.now());
        contract.setStatus(ContractStatus.CONTRACT_PARTLY_USED);

       reader.execute(
                new EditCardFile(
                        contract,
                        contract.getId(),
                        WriteAccessLevel.DEBIT))
               .throwMessageOnError(CardException.class)
               .throwMessageOnAborted(CardException.class);

        reader.execute(
                new SaveEvent(
                        calypsoCardCDMX,
                        TransactionType.SV_CONTRACT_RENEWAL.getValue(),
                        calypsoCardCDMX.getEnvironment().getNetwork().decode(NetworkCode.RFU).getValue(),
                        provider.getValue(),
                        locationId,
                        contract,
                        0,
                        0,
                        calypsoCardCDMX.getEvents().getNextTransactionNumber()
                )).throwMessageOnError(CardException.class)
                .throwMessageOnAborted(CardException.class);

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .message("card contract renewed, expiration date: " + contract.getExpirationDate(daysOffset))
                .data(true)
                .build();
    }

}
