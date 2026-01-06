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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Transaction to reload a card with a specified amount and renew its contract if necessary.
 * <p>
 * This transaction performs the following operations:
 * <ol>
 *   <li>Verifies that a card is present and matches the expected card.</li>
 *   <li>Checks the contract status and validates maximum balance constraints.</li>
 *   <li>Renews the contract if applicable.</li>
 *   <li>Creates a reload event and performs the reload operation on the card.</li>
 * </ol>
 *
 */
@Getter
@RequiredArgsConstructor
public class ReloadCard extends AbstractTransaction<TransactionDataEvent, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int locationId;
    private final int provider;
    private final int passenger;
    private final int amount;

    @Override
    public TransactionResult<TransactionDataEvent> execute(KeypleTransactionContext context) {

        KeypleUtil.reloadCard(context.getCardTransactionManager(), amount);

        Event event = Event.builEvent(
                TransactionType.RELOAD.getValue(),
                calypsoCardCDMX.getEnvironment().getNetwork().getValue(),
                provider,
                contract.getId(),
                passenger,
                calypsoCardCDMX.getEvents().getNextTransactionNumber(),
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
                provider
        );

        return TransactionResult.<TransactionDataEvent>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(transactionDataEvent)
                .message(String.format("Final card balance for '%s': %s", calypsoCardCDMX.getSerial(), calypsoCardCDMX.getBalance() + amount))
                .build();
    }
}
