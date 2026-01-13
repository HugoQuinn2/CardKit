package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
@Getter
public class Personalization extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    private final byte fileId;
    private final int recordNumber;
    private final byte[] data;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        context.getCardTransactionManager()
                .prepareUpdateRecord(fileId, recordNumber, data)
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        return TransactionResult
                .<Boolean>builder()
                .data(true)
                .build();
    }

}
