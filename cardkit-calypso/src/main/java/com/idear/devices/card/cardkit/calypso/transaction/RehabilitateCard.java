package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

public class RehabilitateCard extends Transaction<Boolean, ReaderPCSC> {

    public static final String NAME = "REHABILITATE_CARD";

    private final CalypsoCardCDMX calypsoCardCDMX;

    public RehabilitateCard(CalypsoCardCDMX calypsoCardCDMX) {
        super(NAME);
        this.calypsoCardCDMX = calypsoCardCDMX;
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

        if (calypsoCardCDMX.isEnabled())
            throw new CardException("card already rehabilitate");

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION)
                .prepareRehabilitate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        return null;
    }

}
