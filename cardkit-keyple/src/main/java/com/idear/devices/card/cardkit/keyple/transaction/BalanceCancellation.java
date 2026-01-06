package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Event;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.keyple.TransactionDataEvent;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Represents a transaction that cancels the balance of a Calypso card.
 * <p>
 * This transaction performs a read operation to ensure a card is present,
 * then executes a reload/renew process with a negative amount equivalent
 * to the current balance, effectively setting the card's balance to zero.
 * </p>
 *
 * <p>
 * The cancellation is performed through a {@link ReloadCard} transaction.
 * If the process is successful, the result will contain {@code true} and a status of {@link TransactionStatus#OK}.
 * Otherwise, it will contain {@code false} and a status of {@link TransactionStatus#ERROR}.
 * </p>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 * @see CalypsoCardCDMX
 * @see ReloadCard
 * @see AbstractTransaction
 */
@RequiredArgsConstructor
public class BalanceCancellation extends AbstractTransaction<TransactionDataEvent, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int transactionType;
    private final int locationId;
    private final int provider;
    private final int passenger;

    @Override
    public TransactionResult<TransactionDataEvent> execute(KeypleTransactionContext context) {
        int negativeAmount = calypsoCardCDMX.getBalance() * (-1);

        KeypleUtil.reloadCard(context.getCardTransactionManager(), negativeAmount);

        Event event = Event.builEvent(
                transactionType,
                calypsoCardCDMX.getEnvironment().getNetwork().getValue(),
                provider,
                contract.getId(),
                passenger,
                calypsoCardCDMX.getEvents().getNextTransactionNumber(),
                locationId,
                negativeAmount
        );

        TransactionDataEvent transactionDataEvent = KeypleUtil.saveEvent(
                context.getCardTransactionManager(),
                calypsoCardCDMX,
                context.getKeypleCardReader().getCalypsoCard(),
                context.getKeypleCalypsoSamReader(),
                event,
                contract,
                calypsoCardCDMX.getBalance(),
                provider
        );

        return TransactionResult.<TransactionDataEvent>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(transactionDataEvent)
                .message(String.format("Final card balance for '%s': %s", calypsoCardCDMX.getSerial(), calypsoCardCDMX.getBalance() + negativeAmount))
                .build();

    }

}
