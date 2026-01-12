package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file.Contract;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransactionManager;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.event.CardEvent;
import com.idear.devices.card.cardkit.keyple.event.CardStatus;
import com.idear.devices.card.cardkit.keyple.event.ICardEvent;
import com.idear.devices.card.cardkit.keyple.transaction.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Slf4j
public class KeypleTransactionManager extends AbstractTransactionManager
        <KeypleCardReader, KeypleCalypsoSamReader, KeypleTransactionContext> {

    private volatile  SecureRegularModeTransactionManager ctm;
    private final ExecutorService executorCardMonitor = Executors.newSingleThreadExecutor();
    private final ExecutorService executorCardEvent = Executors.newSingleThreadExecutor();
    private final List<ICardEvent> cardEventList = new ArrayList<>();

    public KeypleTransactionManager(
            KeypleCardReader cardReader,
            KeypleCalypsoSamReader samReader) {
        super(cardReader, samReader);
    }

    private void cardMonitor() {
        while (true) {
            try {
                cardReader.waitForCardPresent(0);
                ctm = KeypleUtil.prepareCardTransactionManger(
                        cardReader.getCardReader(),
                        cardReader.getCalypsoCard(),
                        samReader.getSymmetricCryptoSettingsRT()
                );
                notifyListeners(new CardEvent(cardReader, CardStatus.CARD_PRESENT));
                cardReader.waitForCarAbsent(0);
                ctm.processCommands(ChannelControl.CLOSE_AFTER);
                notifyListeners(new CardEvent(cardReader, CardStatus.CARD_ABSENT));
            } catch (Exception e) {
                log.error("card monitor error", e);
            } finally {
                ctm = null;
            }
        }
    }

    private void notifyListeners(CardEvent event) {
        for (ICardEvent cardEvent : cardEventList)
            executorCardEvent.submit(() -> cardEvent.onEvent(event));
    }

    public void startCardMonitor() {
        executorCardMonitor.submit(this::cardMonitor);
    }

    @Override
    protected KeypleTransactionContext createContext() {
        return KeypleTransactionContext
                .builder()
                .cardTransactionManager(ctm)
                .keypleCalypsoSamReader(samReader)
                .keypleCardReader(cardReader)
                .build();
    }

    public TransactionResult<Boolean> openSession(WriteAccessLevel writeAccessLevel) {
        return execute(new OpenSession(writeAccessLevel));
    }

    public TransactionResult<Boolean> closeSession() {
        return execute(new CloseSession());
    }

    public TransactionResult<CalypsoCardCDMX> readCardData(WriteAccessLevel writeAccessLevel) {
        return execute(new ReadAllCard(writeAccessLevel));
    }

    public TransactionResult<TransactionDataEvent> debitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int transactionType,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return execute(new DebitCard(
                calypsoCardCDMX, contract, transactionType, locationId, provider, passenger, amount));
    }

    public TransactionResult<TransactionDataEvent> debitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            int transactionType,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return debitCard(
                calypsoCardCDMX,
                calypsoCardCDMX.getContracts().getFirstContractValid(),
                transactionType,
                locationId,
                provider,
                passenger,
                amount
        );
    }

    public TransactionResult<TransactionDataEvent> reloadCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return execute(new ReloadCard(calypsoCardCDMX, contract, locationId, provider, passenger, amount));
    }

    public TransactionResult<TransactionDataEvent> reloadCard(
            CalypsoCardCDMX calypsoCardCDMX,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return reloadCard(calypsoCardCDMX, calypsoCardCDMX.getContracts().getFirstContractValid(), locationId, provider, passenger, amount);
    }

    public TransactionResult<Boolean> rehabilitateCard(
            CalypsoCardCDMX calypsoCardCDMX) {
        return execute(new RehabilitateCard(calypsoCardCDMX));
    }

    public TransactionResult<TransactionDataEvent> invalidateCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int transactionType,
            int locationId,
            int provider,
            int passenger) {
        return execute(
                new InvalidateCard(
                        calypsoCardCDMX,
                        contract,
                        transactionType,
                        locationId,
                        provider,
                        passenger)
        );
    }

    public TransactionResult<TransactionDataEvent> invalidateCard(
            CalypsoCardCDMX calypsoCardCDMX,
            int transactionType,
            int locationId,
            int provider,
            int passenger) {
        return invalidateCard(
                calypsoCardCDMX,
                calypsoCardCDMX.getContracts().getFirstContractValid(),
                transactionType,
                locationId,
                provider,
                passenger);
    }

    public TransactionResult<TransactionDataEvent> balanceCancellation(
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int transactionType,
            int locationId,
            int provider,
            int passenger) {
        return execute(new BalanceCancellation(calypsoCardCDMX, contract, transactionType, locationId, provider, passenger));
    }

    public TransactionResult<TransactionDataEvent> balanceCancellation(
            CalypsoCardCDMX calypsoCardCDMX,
            int transactionType,
            int locationId,
            int provider,
            int passenger) {
        return balanceCancellation(calypsoCardCDMX, calypsoCardCDMX.getContracts().getFirstContractValid(), transactionType, locationId, provider, passenger);
    }

    public TransactionResult<TransactionDataEvent> renewContract(
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int locationId,
            int provider,
            int passenger,
            ReverseDate startDate,
            int duration) {
        return execute(new RenewedCard(calypsoCardCDMX, contract, locationId, provider, passenger, startDate, duration));
    }

    public TransactionResult<TransactionDataEvent> renewContract(
            CalypsoCardCDMX calypsoCardCDMX,
            int locationId,
            int provider,
            int passenger,
            ReverseDate startDate,
            int duration) {
        return execute(new RenewedCard(calypsoCardCDMX, calypsoCardCDMX.getContracts().getFirstContractValid(), locationId, provider, passenger, startDate, duration));
    }

    public TransactionResult<TransactionDataEvent> purchaseCard(
            CalypsoCardCDMX calypsoCardCDMX,
            int locationId,
            int contractId,
            int modality,
            int tariff,
            int restrictTime,
            int duration,
            int provider,
            int passenger,
            int amount) {
        return execute(
                new PurchaseCard(
                        calypsoCardCDMX,
                        locationId,
                        contractId,
                        modality,
                        tariff,
                        restrictTime,
                        duration,
                        provider,
                        passenger,
                        amount
                )
        );
    }
    
    public TransactionResult<Boolean> personalization(
            byte fileId,
            int recordNumber,
            byte[] data) {
        return execute(new Personalization(fileId, recordNumber, data));
    }

    public TransactionResult<Boolean> prePersonalization(
            PrePersonalization.KeyGenerated keyGenerated,
            LocalDate startDate,
            LocalDate endDate) {
        return execute(new PrePersonalization(keyGenerated, startDate, endDate));
    }


}
