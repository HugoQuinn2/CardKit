package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

@RequiredArgsConstructor
public class Personalization extends Transaction<Boolean, KeypleReader> {

    private final int recordNumber;
    private final File<?> file;

    @Override
    public TransactionResult<Boolean> execute(KeypleReader reader) {

        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION)
                .prepareUpdateRecord(
                        file.getFileId(),
                        recordNumber,
                        file.unparse())
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        return null;
    }

}
