package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
public class RehabilitateCard extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        if (calypsoCardCDMX.isEnabled())
            throw new CardException("card already rehabilitate");

        KeypleUtil.rehabilitateCard(context.getCardTransactionManager());

        return TransactionResult
                .<Boolean>builder()
                .data(true)
                .message("card " + calypsoCardCDMX.getSerial() + " rehabilitated")
                .build();
    }

}
