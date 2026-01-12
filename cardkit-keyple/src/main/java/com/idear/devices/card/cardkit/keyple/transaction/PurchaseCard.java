package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file.Event;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file.Contract;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.keyple.TransactionDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
public class PurchaseCard extends AbstractTransaction<TransactionDataEvent, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final int locationId;
    private final int contractId;
    private final int modality;
    private final int tariff;
    private final int restrictTime;
    private final int duration;
    private final int provider;
    private final int passenger;
    private final int amount;

    public TransactionResult<TransactionDataEvent> execute(KeypleTransactionContext context) {
        log.info("Purchasing card {}, modality: {}, tariff: {}, expiration: {}, restrict: {}",
                calypsoCardCDMX.getSerial(), modality, tariff, PeriodType.getExpirationDate(LocalDate.now(), duration), restrictTime);

        Contract contract = Contract.buildContract(
                contractId,
                context.getKeypleCalypsoSamReader().getSamNetworkCode(),
                context.getKeypleCalypsoSamReader().getSamProviderCode().getValue(),
                modality,
                tariff,
                restrictTime,
                context.getKeypleCalypsoSamReader().getSerial()
        );

        Contract _contract = KeypleUtil.setupRenewContract(
                context.getCardTransactionManager(),
                HexUtil.toByteArray(calypsoCardCDMX.getSerial()),
                contract,
                provider,
                ReverseDate.now(),
                duration
        );

        KeypleUtil.editCardFile(
                context.getCardTransactionManager(),
                Calypso.CONTRACT_FILE,
                _contract.getId(),
                _contract.unparse(),
                ChannelControl.KEEP_OPEN,
                false
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
                _contract,
                calypsoCardCDMX.getBalance(),
                provider,
                ChannelControl.KEEP_OPEN
        );

        return TransactionResult
                .<TransactionDataEvent>builder()
                .transactionStatus(TransactionStatus.OK)
                .message(String.format("card %s purchase, expiration: %s", calypsoCardCDMX.getSerial(), contract.getExpirationDate()))
                .data(transactionDataEvent)
                .build();
    }

}
