package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

public class InvalidateCard extends Transaction<Boolean, ReaderPCSC> {

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int locationId;
    private TransactionType transactionType = TransactionType.BLACKLISTED_CARD;

    public InvalidateCard(CalypsoCardCDMX calypsoCardCDMX, Contract contract, int locationId) {
        super("invalidate_card");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.contract = contract;
        this.locationId = locationId;
    }

    public InvalidateCard(CalypsoCardCDMX calypsoCardCDMX, int locationId) {
        super("invalidate_card");
        this.calypsoCardCDMX = calypsoCardCDMX;

        this.contract = calypsoCardCDMX.getContracts()
                .findFirst(c -> c.getStatus().isAccepted())
                .orElseThrow(() -> new CardException(
                        "card '%s' without valid contract", calypsoCardCDMX.getSerial()));
        this.locationId = locationId;
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("card already invalidated");

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareInvalidate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.execute(
                new SaveEvent(
                        transactionType,
                        calypsoCardCDMX.getEnvironment(),
                        contract,
                        0,
                        locationId,
                        0,
                        calypsoCardCDMX.getEvents().getNextTransactionNumber()
                ));

        return null;
    }

    public InvalidateCard transactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }
}
