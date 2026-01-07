package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Event;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.DateUtils;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.keyple.TransactionDataEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
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
@RequiredArgsConstructor
public class DebitCard
        extends AbstractTransaction<TransactionDataEvent, KeypleTransactionContext> {

    private static final int MAX_POSSIBLE_AMOUNT = 32767;

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int transactionType;
    private final int locationId;
    private final int provider;
    private final int passenger;
    private final int amount;

    private Tariff tariff;

    @Override
    public TransactionResult<TransactionDataEvent> execute(KeypleTransactionContext context) {

        KeypleUtil.performDebit(
                context.getCardTransactionManager(),
                amount,
                ChannelControl.KEEP_OPEN
        );

        Event event = Event.builEvent(
                transactionType,
                calypsoCardCDMX.getEnvironment().getNetwork().getValue(),
                provider,
                contract.getId(),
                passenger,
                getCalypsoCardCDMX().getEvents().getNextTransactionNumber(),
                locationId,
                amount
        );

        TransactionDataEvent transactionDataEvent = KeypleUtil.saveEvent(
                context.getCardTransactionManager(),
                calypsoCardCDMX,
                context.getKeypleCardReader().getCalypsoCard(),
                context.getKeypleCalypsoSamReader(),
                event,
                contract,
                calypsoCardCDMX.getBalance(),
                provider,
                ChannelControl.KEEP_OPEN
        );

        return TransactionResult
                .<TransactionDataEvent>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(transactionDataEvent)
                .build();
    }

    /**
     * Validates the passback time between two consecutive debits.
     *
     * @param now             Current timestamp.
     * @param lastDebitDateTime Timestamp of the last debit.
     * @throws CardException if the passback time is too short.
     */
    private void validatePassback(LocalDateTime now, LocalDateTime lastDebitDateTime) {
        int passBackDuration = calypsoCardCDMX.getEnvironment().getProfile().decode(Profile.RFU).getPassBack();
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
     * Validates whether the current profile is allowed on the given equipment.
     *
     * @param equipment The device performing the debit.
     * @throws CardException if the profile is not allowed on the equipment.
     */
    private void validateProfileOnEquipment(Equipment equipment) {
        if (!calypsoCardCDMX.getEnvironment().getProfile().decode(Profile.RFU).isAllowedOn(equipment)) {
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
    private void validateContract(int deviceProvider) {
        if (!contract.getStatus().decode(ContractStatus.RFU).isAccepted())
            throw new CardException("card without valid contract");

        if (contract.isExpired(0))
            throw new CardException("card expired on %s", contract.getExpirationDate());

        if (contract.getModality().decode(Modality.FORBIDDEN) == Modality.MONOMODAL &&
                contract.getProvider().getValue() != provider) {
            throw new CardException("monomodal verification failed, device provider must match contract provider %s",
                    contract.getProvider());
        }

        if (contract.getTariff().decode(Tariff.RFU) == Tariff.SEASON_PASS ||
                contract.getTariff().decode(Tariff.RFU) == Tariff.TICKET_BOOK)
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
//        if (!contract.getTariff().equals(Tariff.STORED_VALUE)) finalAmount = 0;
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

        log.info(message);
        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message(message)
                .build();
    }
}
