package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
@Getter
public class AppendPersonalization extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    private final byte fileId;
    private final byte[] data;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        KeypleUtil.appendEditCardFile(
                context.getCardTransactionManager(),
                WriteAccessLevel.PERSONALIZATION,
                fileId,
                data,
                ChannelControl.KEEP_OPEN
        );

        return TransactionResult
                .<Boolean>builder()
                .data(true)
                .build();
    }
}
