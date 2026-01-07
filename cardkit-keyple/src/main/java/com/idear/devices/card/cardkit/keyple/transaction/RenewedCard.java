package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Event;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.keyple.TransactionDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.CardTransactionLegacySamExtension;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.TraceableSignatureComputationData;

import java.util.Arrays;

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
@RequiredArgsConstructor
@Slf4j
public class RenewedCard extends AbstractTransaction<TransactionDataEvent, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int locationId;
    private final int provider;
    private final int passenger;
    private final ReverseDate startDate;
    private final int duration;

    @Override
    public TransactionResult<TransactionDataEvent> execute(KeypleTransactionContext context) {
        log.info("Renewing card {}, contract id: {}, start date: {}, expiration: {}",
                calypsoCardCDMX.getSerial(), contract.getId(), startDate.getDate(),
                PeriodType.getExpirationDate(startDate, duration));

        Contract _contract = KeypleUtil.setupRenewContract(
                context.getCardTransactionManager(),
                HexUtil.toByteArray(calypsoCardCDMX.getSerial()),
                contract,
                provider,
                startDate,
                duration
        );

        KeypleUtil.editCardFile(
                context.getCardTransactionManager(),
                WriteAccessLevel.DEBIT,
                _contract.getFileId(),
                _contract.getId(),
                _contract.unparse(),
                ChannelControl.KEEP_OPEN
        );

        Event event = Event.builEvent(
                TransactionType.SV_CONTRACT_RENEWAL.getValue(),
                calypsoCardCDMX.getEnvironment().getNetwork().getValue(),
                provider,
                _contract.getId(),
                passenger,
                calypsoCardCDMX.getEvents().getNextTransactionNumber(),
                locationId,
                0
        );


        TransactionDataEvent transactionDataEvent = KeypleUtil.saveEvent(
                context.getCardTransactionManager(),
                calypsoCardCDMX,
                context.getKeypleCardReader().getCalypsoCard(),
                context.getKeypleCalypsoSamReader(),
                event,
                _contract,
                calypsoCardCDMX.getBalance(),
                provider,
                ChannelControl.KEEP_OPEN
        );

        return TransactionResult
                .<TransactionDataEvent>builder()
                .transactionStatus(TransactionStatus.OK)
                .message("card contract renewed, expiration date: " + _contract.getExpirationDate(0))
                .data(transactionDataEvent)
                .build();
    }

}
