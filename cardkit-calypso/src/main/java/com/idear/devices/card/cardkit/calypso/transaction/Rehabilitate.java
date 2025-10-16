package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

public class Rehabilitate extends Transaction<Boolean, ReaderPCSC> {

    private final CalypsoCardCDMX calypsoCardCDMX;

    public Rehabilitate(CalypsoCardCDMX calypsoCardCDMX) {
        super("rehabilitate_card");
        this.calypsoCardCDMX = calypsoCardCDMX;
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

        if (calypsoCardCDMX.isEnabled())
            throw new CardException("card already rehabilitate");

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareRehabilitate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        return null;
    }

}
