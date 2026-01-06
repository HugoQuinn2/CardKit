package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransactionManager;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.transaction.*;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;

public class KeypleTransactionManager extends AbstractTransactionManager
        <KeypleCardReader, KeypleCalypsoSamReader, KeypleTransactionContext> {

    private final String aid;
    private SecureRegularModeTransactionManager ctm;
    private CalypsoCard lastCalypsoCard;

    public KeypleTransactionManager(
            KeypleCardReader cardReader,
            KeypleCalypsoSamReader samReader,
            String aid) {
        super(cardReader, samReader);
        this.aid = aid;
    }

    @Override
    protected KeypleTransactionContext createContext() {
        cardReader.connectToCard();
        if (lastCalypsoCard == null || lastCalypsoCard != cardReader.getCalypsoCard()) {
            ctm = KeypleUtil.prepareCardTransactionManger(
                    cardReader.getCardReader(),
                    cardReader.getCalypsoCard(),
                    samReader.getSymmetricCryptoSettingsRT()
            );
            lastCalypsoCard = cardReader.getCalypsoCard();
        }

        return KeypleTransactionContext
                .builder()
                .cardTransactionManager(ctm)
                .keypleCalypsoSamReader(samReader)
                .keypleCardReader(cardReader)
                .build();
    }

    public TransactionResult<CalypsoCardCDMX> readCardData() {
        return execute(new ReadAllCard());
    }

    public TransactionResult<TransactionDataEvent> debitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return execute(new DebitCard(
                calypsoCardCDMX, contract, locationId, provider, passenger, amount));
    }

    public TransactionResult<TransactionDataEvent> debitCard(
            CalypsoCardCDMX calypsoCardCDMX,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return debitCard(
                calypsoCardCDMX,
                calypsoCardCDMX.getContracts().getFirstContractValid(),
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
            Contract contract,
            int locationId,
            int provider,
            int passenger,
            int amount) {
        return execute(new SellCard(calypsoCardCDMX, contract, locationId, provider, passenger, amount));
    }
    
    public TransactionResult<Boolean> personalization(File<?> file, int recordNumber) {
        return execute(new Personalization(recordNumber, file));
    }


}
