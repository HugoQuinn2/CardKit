package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.CalypsoProduct;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.*;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import lombok.var;
import lombok.var;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.ElementaryFile;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

import java.util.SortedMap;

/**
 * Represents a transaction that performs a complete read operation on a Calypso card.
 *
 * <p>
 * The result contains the fully populated {@link CalypsoCardCDMX} instance if the
 * operation is successful. If any step fails, a {@link ReaderException} or
 * {@link CardException} is thrown, depending on the cause.
 * </p>
 *
 * @see CalypsoCardCDMX
 * @see KeypleCardReader
 * @see AbstractTransaction
 * @see TransactionResult
 * @see TransactionStatus
 * @see WriteAccessLevel
 *
 * @author Victor Hugo Gaspar Quinn
 */
@Slf4j
@NoArgsConstructor
public class ReadAllCard extends AbstractTransaction<CalypsoCardCDMX, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX = new CalypsoCardCDMX();

    @Override
    public TransactionResult<CalypsoCardCDMX> execute(KeypleTransactionContext context) {
        CalypsoCard calypsoCard = context.getKeypleCardReader().getCalypsoCard();
        calypsoCardCDMX.setEnabled(!calypsoCard.isDfInvalidated());
        calypsoCardCDMX.setSerial(HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));
        calypsoCardCDMX.setCalypsoProduct(CalypsoProduct.parseByCalypsoCard(calypsoCard));

        log.info("Reading card {}", calypsoCardCDMX.getSerial());

        try {
            context.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareReadRecord(Calypso.ENVIRONMENT_FILE, 1)
                    .prepareReadRecordsPartially(Calypso.EVENT_FILE, 1, 3, 0, 29)
                    .prepareReadRecordsPartially(Calypso.CONTRACT_FILE, 1, 8, 0, 29)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);

            calypsoCardCDMX.setBalance(calypsoCard.getSvBalance());
            ElementaryFile elementaryFileEnv = calypsoCard.getFileBySfi(Calypso.ENVIRONMENT_FILE);
            calypsoCardCDMX.setEnvironment(new Environment().parse(elementaryFileEnv != null ? elementaryFileEnv.getData().getContent() : null));

            readEventFiles(calypsoCard);
            readContractFiles(calypsoCard);
            readLogFiles(calypsoCard);
        } catch (Exception exception) {
            log.debug("Error reading files card data: {}", exception.getMessage());
        }

        return TransactionResult
                .<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCardCDMX)
                .message("All card data '" + calypsoCardCDMX.getSerial() + "' was read")
                .build();
    }

    private void readLogFiles(
            CalypsoCard calypsoCard) {
        calypsoCardCDMX.setDebitLog(new DebitLog().parse(calypsoCard.getSvDebitLogLastRecord()));
        calypsoCardCDMX.setLoadLog(new LoadLog().parse(calypsoCard.getSvLoadLogRecord()));
    }

    private void readEventFiles(CalypsoCard calypsoCard) {
        SortedMap<Integer, byte[]> readFilesBytes = calypsoCard.getFileBySfi(Calypso.EVENT_FILE)
                .getData()
                .getAllRecordsContent();

        if (readFilesBytes == null)
            return;

        Events events = new Events();
        for (var entry : readFilesBytes.entrySet()) {
            events.add(new Event(entry.getKey()).parse(entry.getValue()));
        }
        calypsoCardCDMX.setEvents(events);
    }

    private void readContractFiles(CalypsoCard calypsoCard) {
        SortedMap<Integer, byte[]> readFilesBytes = calypsoCard.getFileBySfi(Calypso.CONTRACT_FILE)
                .getData()
                .getAllRecordsContent();

        if (readFilesBytes == null)
            return;

        Contracts contracts = new Contracts();
        for (var entry : readFilesBytes.entrySet()) {
            contracts.add(new Contract(entry.getKey()).parse(entry.getValue()));
        }
        calypsoCardCDMX.setContracts(contracts);
    }

}
