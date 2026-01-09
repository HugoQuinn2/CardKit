package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
public class OpenSession extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    private final WriteAccessLevel writeAccessLevel;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        context.getCardTransactionManager()
                .prepareOpenSecureSession(writeAccessLevel)
                .processCommands(ChannelControl.KEEP_OPEN);

        return TransactionResult
                .<Boolean>builder()
                .data(true)
                .build();
    }
}
