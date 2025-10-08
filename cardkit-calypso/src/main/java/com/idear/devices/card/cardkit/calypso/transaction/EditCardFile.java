package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;

public class EditCardFile extends Transaction<byte[], ReaderPCSC> {

    private final File<?> file;
    private final int recordNumber;

    public EditCardFile(File<?> file, int recordNumber) {
        super("edit file");
        this.file = file;
        this.recordNumber = recordNumber;
    }

    @Override
    public TransactionResult<byte[]> execute(ReaderPCSC reader) {
        TransactionResult<CalypsoCardCDMX> simpleRead = reader.execute(new SimpleReadCard());

        if (!simpleRead.isOk())
            throw new ReaderException("no card on reader");

        SecureRegularModeTransactionManager cardTransactionManager =
                ReaderPCSC.calypsoCardApiFactory
                        .createSecureRegularModeTransactionManager(
                                reader.getCardReader(),
                                reader.getCalypsoCard(),
                                reader.getCalypsoSam().getSymmetricCryptoSettingsRT()
                        );

        // edit file card
        cardTransactionManager
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareUpdateRecord(
                        file.getFileId(),
                        recordNumber,
                        file.unparse()
                )
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        // read card file and returned
        TransactionResult<byte[]> readFile = reader.execute(new ReadCardFile(WriteAccessLevel.DEBIT, file.getFileId(), recordNumber));
        if (!readFile.isOk())
            throw new CardException("no se pudo leer el archivo %s: %s", file.getFileId(), readFile.getMessage());

        return TransactionResult
                .<byte[]>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(readFile.getData())
                .build();
    }

}
