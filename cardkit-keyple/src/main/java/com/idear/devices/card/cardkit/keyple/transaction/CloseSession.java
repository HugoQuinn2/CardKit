package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

public class CloseSession extends AbstractTransaction<Boolean, KeypleTransactionContext> {
    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        context.getCardTransactionManager()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER);

        return TransactionResult
                .<Boolean>builder()
                .data(true)
                .build();
    }
}
