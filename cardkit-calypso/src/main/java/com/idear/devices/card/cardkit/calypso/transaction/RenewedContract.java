package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.core.datamodel.ReverseDate;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;

import java.time.LocalDate;
import java.util.Optional;

public class RenewedContract extends Transaction<Boolean, ReaderPCSC> {

    private final Contract contract;
    private final int daysOffset;
    private int duration = 60;

    private final TransactionType transactionType = TransactionType.SV_CONTRACT_RENEWAL;

    public RenewedContract(Contract contract, int daysOffset) {
        super("renewed contract");
        this.contract = contract;
        this.daysOffset = daysOffset;
    }

    public RenewedContract setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        TransactionResult<CalypsoCardCDMX> simpleRead = reader.execute(new SimpleReadCard());

        if (!simpleRead.isOk())
            throw new CardException("no card on reader");

        if (contract.isExpired(daysOffset)) {

            contract.setDuration(duration);

            contract.setStartDate(new ReverseDate(
                    ReverseDate.toInt(LocalDate.now())
            ));

            TransactionResult<byte[]> editResult = reader.execute(new EditCardFile(contract, 1));

            if (editResult.isOk())
                return TransactionResult
                        .<Boolean>builder()
                        .transactionStatus(TransactionStatus.OK)
                        .message("card contract renewed expiration date: " )
                        .data(true)
                        .build();
        }

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .message("card contract does not require renewal expiration date: " +
                        contract.getSaleDate().getDate().plusMonths(
                                contract.getDuration())
                                .minusDays(daysOffset))
                .data(true)
                .build();
    }

}
