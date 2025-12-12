package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.EditCardFile;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;

import java.util.Optional;

@RequiredArgsConstructor
public class SellCard extends Transaction<Boolean, KeypleReader> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final LocationCode locationCode;
    private final Provider provider;
    private final Modality modality;
    private final RestrictTime restrictTime;
    private final int amount;

    @Override
    public TransactionResult<Boolean> execute(KeypleReader reader) {
        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("card invalidated");

        Profile profile = calypsoCardCDMX.getEnvironment().getProfile().decode(Profile.RFU);

        Optional<Contract> optionalContract = calypsoCardCDMX.getContracts()
                .findFirst(c -> c.getStatus().decode().isAccepted());
        Contract contract;
        contract = optionalContract.orElseGet(() -> Contract.buildContract(
                1,
                1,
                calypsoCardCDMX.getEnvironment().getNetwork().decode(NetworkCode.RFU),
                provider,
                modality,
                profile.getTariff(),
                restrictTime,
                Integer.parseInt(reader.getKeypleCalypsoSam().getSerial(), 16)
        ));

        contract.setDuration(profile.getValidityContract());
        contract.setStartDate(ReverseDate.now());
        contract.getStatus().setValue(ContractStatus.CONTRACT_PARTLY_USED);

        reader.execute(
                new EditCardFile(
                        contract,
                        contract.getId(),
                        WriteAccessLevel.DEBIT))
                .throwMessageOnAborted(CardException.class)
                .throwMessageOnError(CardException.class);

        reader.execute(
                new SaveEvent(
                        calypsoCardCDMX,
                        TransactionType.CARD_PURCHASE.getValue(),
                        calypsoCardCDMX.getEnvironment().getNetwork().getValue(),
                        provider.getValue(),
                        locationCode,
                        contract,
                        0,
                        amount,
                        calypsoCardCDMX.getEvents().getNextTransactionNumber()
                ));

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .message("card contract renewed, expiration date: " + contract.getExpirationDate(0))
                .data(true)
                .build();
    }
}
