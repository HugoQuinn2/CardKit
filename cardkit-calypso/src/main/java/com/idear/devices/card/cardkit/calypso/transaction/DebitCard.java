package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.datamodel.location.StationCode;
import com.idear.devices.card.cardkit.core.datamodel.location.TransportLocation;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Represents a debit transaction on a Calypso card.
 * <p>
 * Supports multi-debit for amounts exceeding {@link #MAX_POSSIBLE_AMOUNT} and handles:
 * <ul>
 *     <li>Passback validation</li>
 *     <li>Contract status and expiration check</li>
 *     <li>Profile and equipment validation</li>
 *     <li>Unsupported tariff rejection</li>
 *     <li>Event recording for special services and free passes</li>
 * </ul>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
@Getter
@Slf4j
public class DebitCard extends Transaction<Boolean, ReaderPCSC> {

    private static final int MAX_POSSIBLE_AMOUNT = 32767;

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Provider deviceProvider;
    private final Contract contract;
    private final int passenger;
    private final LocationCode locationId;
    private final int amount;

    private int contractDaysOffset = 0;

    /**
     * Creates a new debit transaction with a known contract.
     *
     * @param calypsoCardCDMX The Calypso card instance.
     * @param deviceProvider  The provider of the device performing the debit.
     * @param contract        The contract associated with the card.
     * @param passenger       The passenger ID.
     * @param amount          The amount to debit.
     * @param locationId      The location of the transaction.
     */
    public DebitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Provider deviceProvider,
            LocationCode locationId,
            int amount,
            int passenger,
            Contract contract) {
        super("debit card");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.deviceProvider = deviceProvider;
        this.contract = contract;
        this.passenger = passenger;
        this.amount = amount;
        this.locationId = locationId;
    }

    /**
     * Creates a new debit transaction, automatically resolving the first accepted contract.
     *
     * @param calypsoCardCDMX The Calypso card instance.
     * @param deviceProvider  The provider of the device performing the debit.
     * @param passenger       The passenger ID.
     * @param amount          The amount to debit.
     * @param locationId      The location of the transaction.
     */
    public DebitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Provider deviceProvider,
            LocationCode locationId,
            int amount,
            int passenger) {
        super("debit card");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.deviceProvider = deviceProvider;
        this.passenger = passenger;
        this.amount = amount;
        this.locationId = locationId;

        this.contract = calypsoCardCDMX.getContracts()
                .findFirst(c -> c.getStatus().isAccepted())
                .orElseThrow(() -> new CardException(
                        "card '%s' without valid contract", calypsoCardCDMX.getSerial()));
    }

    /**
     * Creates a new debit transaction, automatically resolving the first accepted contract and default passenger 0.
     *
     * @param calypsoCardCDMX The Calypso card instance.
     * @param deviceProvider  The provider of the device performing the debit.
     * @param amount          The amount to debit.
     * @param locationId      The location of the transaction.
     */
    public DebitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Provider deviceProvider,
            LocationCode locationId,
            int amount) {
        this(calypsoCardCDMX, deviceProvider, locationId, amount, 0);
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        log.info("Debiting card {}.", calypsoCardCDMX.getSerial());

        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("disabled card");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastDebitDateTime = LocalDateTime.of(
                calypsoCardCDMX.getDebitLog().getDate().getDate(),
                calypsoCardCDMX.getDebitLog().getTime().getTime()
        );

        validatePassback(now, lastDebitDateTime);

        Equipment equipment = resolveEquipment(locationId);
        validateProfileOnEquipment(equipment);
        validateContract(deviceProvider);

        int finalAmount = resolveDebitAmount();
        performDebit(reader, finalAmount);

        TransactionType transactionType = determineTransactionType(finalAmount, equipment);
        recordEvent(reader, transactionType, finalAmount);

        return buildTransactionResult(finalAmount);
    }

    /**
     * Validates the passback time between two consecutive debits.
     *
     * @param now             Current timestamp.
     * @param lastDebitDateTime Timestamp of the last debit.
     * @throws CardException if the passback time is too short.
     */
    private void validatePassback(LocalDateTime now, LocalDateTime lastDebitDateTime) {
        int passBackDuration = calypsoCardCDMX.getEnvironment().getProfile().getPassBack();
        Duration passBack = Duration.ofMinutes(passBackDuration);

        if (Duration.between(lastDebitDateTime, now).compareTo(passBack) < 0) {
            throw new CardException("invalid pass back of %s minutes for profile %s, wait until %s, last debit on %s",
                    passBackDuration,
                    calypsoCardCDMX.getEnvironment().getProfile(),
                    DateUtils.toLocalZone(lastDebitDateTime.plusMinutes(passBackDuration)),
                    DateUtils.toLocalZone(lastDebitDateTime));
        }
    }

    /**
     * Resolves the equipment for a given transaction location.
     *
     * @param location The transaction location.
     * @return The equipment associated with the location.
     */
    private Equipment resolveEquipment(LocationCode location) {
        if (location instanceof StationCode) return ((StationCode) location).getEquipment();
        if (location instanceof TransportLocation) return ((TransportLocation) location).getEquipment();
        return Equipment.RFU;
    }

    /**
     * Validates whether the current profile is allowed on the given equipment.
     *
     * @param equipment The device performing the debit.
     * @throws CardException if the profile is not allowed on the equipment.
     */
    private void validateProfileOnEquipment(Equipment equipment) {
        if (!calypsoCardCDMX.getEnvironment().getProfile().isAllowedOn(equipment)) {
            throw new CardException("profile %s is not allowed on this device [%s]",
                    calypsoCardCDMX.getEnvironment().getProfile(),
                    equipment);
        }
    }

    /**
     * Validates contract status, expiration, modality, and tariff restrictions.
     *
     * @param deviceProvider The provider performing the transaction.
     * @throws CardException if contract validations fail.
     */
    private void validateContract(Provider deviceProvider) {
        if (!contract.getStatus().isAccepted())
            throw new CardException("card without valid contract");

        if (contract.isExpired(contractDaysOffset))
            throw new CardException("card expired on %s", contract.getExpirationDate(contractDaysOffset));

        if (contract.getModality() == Modality.MONOMODAL &&
                !deviceProvider.equals(contract.getProvider())) {
            throw new CardException("monomodal verification failed, device provider must match contract provider %s",
                    contract.getProvider());
        }

        if (contract.getTariff() == Tariff.SEASON_PASS || contract.getTariff() == Tariff.TICKET_BOOK)
            throw new CardException("unsupported tariff %s on this transaction", contract.getTariff());
    }

    /**
     * Determines the final debit amount based on the contract tariff and max allowed amount.
     *
     * @return The final debit amount.
     * @throws CardException if the amount exceeds {@link #MAX_POSSIBLE_AMOUNT}.
     */
    private int resolveDebitAmount() {
        int finalAmount = amount;
        if (!contract.getTariff().equals(Tariff.STORED_VALUE)) finalAmount = 0;
        if (finalAmount > MAX_POSSIBLE_AMOUNT)
            throw new CardException("amount cannot be greater than %s", MAX_POSSIBLE_AMOUNT);

        if (finalAmount > calypsoCardCDMX.getBalance())
            throw new CardException("insufficient balance for debit, amount %s, current balance %s", finalAmount, calypsoCardCDMX.getBalance());

        return finalAmount;
    }

    /**
     * Determines the transaction type based on amount and equipment type.
     *
     * @param finalAmount The final debit amount.
     * @param equipment   The equipment performing the transaction.
     * @return The transaction type.
     */
    private TransactionType determineTransactionType(int finalAmount, Equipment equipment) {
        if (finalAmount == 0) return TransactionType.MULTIMODAL_FREE_PASS;
        if (equipment.isSpecialService()) return TransactionType.SPECIAL_SERVICE_DEBIT;
        return TransactionType.GENERAL_DEBIT;
    }

    /**
     * Records a transaction event on the card.
     *
     * @param reader          The card reader.
     * @param transactionType The type of the transaction.
     * @param finalAmount     The debit amount.
     */
    private void recordEvent(ReaderPCSC reader, TransactionType transactionType, int finalAmount) {
        reader.execute(new SaveEvent(
                transactionType,
                calypsoCardCDMX.getEnvironment(),
                contract,
                passenger,
                locationId.getCode(),
                finalAmount,
                calypsoCardCDMX.getEvents().getNextTransactionNumber()
        ));
    }

    /**
     * Builds a TransactionResult object containing the transaction outcome and message.
     *
     * @param finalAmount The final debit amount.
     * @return TransactionResult with status and message.
     */
    private TransactionResult<Boolean> buildTransactionResult(int finalAmount) {
        String message = (finalAmount == 0)
                ? String.format("free pass for tariff %s", contract.getTariff())
                : String.format("%s debit to card '%s' was made correctly, final balance: %s",
                amount, calypsoCardCDMX.getSerial(), calypsoCardCDMX.getBalance() - finalAmount);

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message(message)
                .build();
    }

    /**
     * Performs the actual debit operation on the card.
     *
     * @param reader      The card reader.
     * @param debitAmount The amount to debit.
     */
    private void performDebit(ReaderPCSC reader, int debitAmount) {
//        reader.getCardTransactionManager()
//                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
//                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
//                .processCommands(ChannelControl.KEEP_OPEN);

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                .prepareSvDebit(
                        debitAmount,
                        CompactDate.now().toBytes(),
                        CompactTime.now().toBytes())
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);
    }

    public DebitCard contractDaysOffset(int contractDaysOffset) {
        this.contractDaysOffset = contractDaysOffset;
        return this;
    }
}
