package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.NetworkCode;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

public class InvalidateCard extends Transaction<Boolean, KeypleReader> {

    public static final String NAME = "INVALIDATE_CARD";

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Provider provider;
    private final Contract contract;
    private final LocationCode locationId;
    private TransactionType transactionType = TransactionType.BLACKLISTED_CARD;

    public InvalidateCard(
            CalypsoCardCDMX calypsoCardCDMX,
            NetworkCode networkCode,
            Provider provider,
            LocationCode locationId,
            Contract contract) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.provider = provider;
        this.contract = contract;
        this.locationId = locationId;
    }

    public InvalidateCard(
            CalypsoCardCDMX calypsoCardCDMX,
            NetworkCode networkCode,
            Provider provider,
            LocationCode locationId) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;

        this.contract = calypsoCardCDMX.getContracts()
                .findFirst(c -> c.getStatus().isAccepted())
                .orElseThrow(() -> new CardException(
                        "card '%s' without valid contract", calypsoCardCDMX.getSerial()));
        this.provider = provider;
        this.locationId = locationId;
    }

    @Override
    public TransactionResult<Boolean> execute(KeypleReader reader) {

        if (!calypsoCardCDMX.isEnabled())
            throw new CardException("card already invalidated");

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareInvalidate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.execute(
                new SaveEvent(
                        calypsoCardCDMX,
                        transactionType.getValue(),
                        calypsoCardCDMX.getEnvironment().getNetwork().decode(NetworkCode.RFU).getValue(),
                        provider.getValue(),
                        locationId,
                        contract,
                        0,
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
