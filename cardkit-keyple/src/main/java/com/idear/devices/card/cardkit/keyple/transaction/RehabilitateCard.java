package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
public class RehabilitateCard extends Transaction<Boolean, KeypleReader> {

    private final CalypsoCardCDMX calypsoCardCDMX;

    @Override
    public TransactionResult<Boolean> execute(KeypleReader reader) {

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
