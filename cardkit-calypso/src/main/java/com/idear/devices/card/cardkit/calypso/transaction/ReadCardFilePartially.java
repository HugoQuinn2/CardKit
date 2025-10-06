package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;

public class ReadCardFilePartially extends Transaction<CalypsoCard, ReaderPCSC> {

    private final byte fileId;
    private final byte fromRecord;
    private final byte toRecord;
    private final int offset;
    private final int bytesToRead;

    public ReadCardFilePartially(byte fileId, byte fromRecord, byte toRecord, int offset, int bytesToRead) {
        super("files parity");
        this.fileId = fileId;
        this.fromRecord = fromRecord;
        this.toRecord = toRecord;
        this.offset = offset;
        this.bytesToRead = bytesToRead;
    }

    @Override
    public TransactionResult<CalypsoCard> execute(ReaderPCSC reader) {

        TransactionResult<CalypsoCardCDMX> simpleRead = reader.executeTransaction(new SimpleReadCard());
        CalypsoCard calypsoCard = reader.getCalypsoCard();

        SecureRegularModeTransactionManager cardTransactionManager =
                ReaderPCSC.calypsoCardApiFactory
                        .createSecureRegularModeTransactionManager(
                                reader.getCardReader(),
                                calypsoCard,
                                reader.getCalypsoSam().getSymmetricCryptoSettingsRT()
                        );

        cardTransactionManager
                .prepareReadRecordsPartially(
                        fileId,
                        fromRecord,
                        toRecord,
                        offset,
                        bytesToRead)
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.setCalypsoCard(calypsoCard);
        return TransactionResult
                .<CalypsoCard>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCard)
                .build();
    }
}
