package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Event;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.keyple.TransactionDataEvent;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;

@RequiredArgsConstructor
public class PurchaseCard extends AbstractTransaction<TransactionDataEvent, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int locationId;
    private final int provider;
    private final int passenger;
    private final int amount;

    public TransactionResult<TransactionDataEvent> execute(KeypleTransactionContext context) {

        contract.setDuration(PeriodType.encode(PeriodType.MONTH, 60));
//        contract.setStartDate(ReverseDate.now());
//        contract.getStatus().setValue(ContractStatus.CONTRACT_PARTLY_USED);
//        contract.setSaleSam(context.getKeypleCalypsoSamReader().getSerial());

        KeypleUtil.editCardFile(
                context.getCardTransactionManager(),
                WriteAccessLevel.PERSONALIZATION,
                Calypso.CONTRACT_FILE,
                contract.getId(),
                contract.unparse()
        );

        Event event = Event.builEvent(
                TransactionType.CARD_PURCHASE.getValue(),
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

        return TransactionResult
                .<TransactionDataEvent>builder()
                .transactionStatus(TransactionStatus.OK)
                .message(String.format("card %s purchase, expiration: %s", calypsoCardCDMX.getSerial(), contract.getExpirationDate()))
                .data(transactionDataEvent)
                .build();
    }

}
