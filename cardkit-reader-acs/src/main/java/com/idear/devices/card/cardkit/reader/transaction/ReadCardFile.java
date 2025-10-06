package com.idear.devices.card.cardkit.reader.transaction;

import com.idear.devices.card.cardkit.core.io.reader.Reader;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.reader.CalypsoCDMXCard;
import com.idear.devices.card.cardkit.reader.ReaderPCSC;
import lombok.Getter;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

@Getter
public class ReadCardFile extends Transaction<CalypsoCard, ReaderPCSC> {

    private final WriteAccessLevel writeAccessLevel;
    private final byte fileId;
    private final int record;

    public ReadCardFile(WriteAccessLevel writeAccessLevel, byte fileId, int record) {
        super("read file");
        this.writeAccessLevel = writeAccessLevel;
        this.fileId = fileId;
        this.record = record;
    }

    @Override
    public TransactionResult<CalypsoCard> execute(ReaderPCSC reader) {
        TransactionResult<CalypsoCDMXCard> simpleRead = reader.executeTransaction(new SimpleReadCard());
        CalypsoCard calypsoCard = reader.getCalypsoCard();

        SecureRegularModeTransactionManager cardTransactionManager =
                ReaderPCSC.calypsoCardApiFactory
                        .createSecureRegularModeTransactionManager(
                                reader.getCardReader(),
                                calypsoCard,
                                reader.getCalypsoSam().getSymmetricCryptoSettingsRT()
                        );

        cardTransactionManager
                .prepareOpenSecureSession(writeAccessLevel)
                .prepareReadRecord(fileId, record)
                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.setCalypsoCard(calypsoCard);
        return TransactionResult
                .<CalypsoCard>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCard)
                .build();
    }
}
