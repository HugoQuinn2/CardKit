package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;

public class Personalization extends Transaction<Boolean, ReaderPCSC> {

    private final int recordNumber;
    private final File<?> file;

    public Personalization(int recordNumber, File<?> file) {
        super("PERSONALIZATION");
        this.recordNumber = recordNumber;
        this.file = file;
    }

    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {

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
