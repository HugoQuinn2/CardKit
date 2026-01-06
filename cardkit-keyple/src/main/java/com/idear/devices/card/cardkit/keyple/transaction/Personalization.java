package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
public class Personalization extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    private final int recordNumber;
    private final File<?> file;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        KeypleUtil.updateRecord(
                context.getCardTransactionManager(),
                file,
                recordNumber
        );

        return TransactionResult
                .<Boolean>builder()
                .data(true)
                .build();
    }

}
