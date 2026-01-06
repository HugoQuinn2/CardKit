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
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;

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

    private CalypsoCardCDMX calypsoCardCDMX;
    private TransactionResult<byte[]> readFile;
    private TransactionResult<SortedMap<Integer, byte[]>> readFiles;

    @Override
    public TransactionResult<CalypsoCardCDMX> execute(KeypleTransactionContext context) {
        // Initialize the card model to store the read data
        calypsoCardCDMX = new CalypsoCardCDMX();
        SecureRegularModeTransactionManager ctm = context.getCardTransactionManager();

        // Ensure the reader session is up-to-date
        CalypsoCard calypsoCard = context.getKeypleCardReader().getCalypsoCard();

        // Check if the Dedicated File (DF) is invalid
        calypsoCardCDMX.setEnabled(!calypsoCard.isDfInvalidated());

        // Extract and set the card serial number
        calypsoCardCDMX.setSerial(HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));
        log.info("Reading card {}", calypsoCardCDMX.getSerial());

        readLogFiles(ctm, calypsoCard);
        readEnvironmentFile(context);
        readEventFiles(context);
        readContractFiles(context);

        calypsoCardCDMX.setCalypsoProduct(CalypsoProduct.parseByCalypsoCard(calypsoCard));

//        cardTransactionManager.processCommands(ChannelControl.CLOSE_AFTER);
        return TransactionResult
                .<CalypsoCardCDMX>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCardCDMX)
                .message("All card data '" + calypsoCardCDMX.getSerial() + "' was read")
                .build();
    }

    private void readLogFiles(
            SecureRegularModeTransactionManager ctm,
            CalypsoCard calypsoCard) {
        Logs logs = KeypleUtil.readCardLogs(ctm, calypsoCard);
        calypsoCardCDMX.setDebitLog(logs.getDebitLog());
        calypsoCardCDMX.setLoadLog(logs.getLoadLog());
        calypsoCardCDMX.setBalance(calypsoCard.getSvBalance());
    }

    private void readEnvironmentFile(KeypleTransactionContext context) {
        byte[] bytesFiles = KeypleUtil.readCardFile(
                context.getCardTransactionManager(),
                context.getKeypleCardReader().getCalypsoCard(),
                WriteAccessLevel.DEBIT,
                Calypso.ENVIRONMENT_FILE,
                1
        );

        calypsoCardCDMX.setEnvironment(new Environment().parse(bytesFiles));
    }

    private void readEventFiles(KeypleTransactionContext context) {
        SortedMap<Integer, byte[]> readFilesBytes = KeypleUtil.readCardPartially(
                context.getCardTransactionManager(),
                context.getKeypleCardReader().getCalypsoCard(),
                WriteAccessLevel.DEBIT,
                Calypso.EVENT_FILE,
                (byte) 1,
                (byte) 3,
                0,
                29
        );

        Events events = new Events();
        for (var entry : readFilesBytes.entrySet()) {
            events.add(new Event(entry.getKey()).parse(entry.getValue()));
        }
        calypsoCardCDMX.setEvents(events);
    }

    private void readContractFiles(KeypleTransactionContext context) {
        SortedMap<Integer, byte[]> readFilesBytes = KeypleUtil.readCardPartially(
                context.getCardTransactionManager(),
                context.getKeypleCardReader().getCalypsoCard(),
                WriteAccessLevel.DEBIT,
                Calypso.EVENT_FILE,
                (byte) 1,
                (byte) 3,
                0,
                29
        );

        Contracts contracts = new Contracts();
        for (var entry : readFilesBytes.entrySet()) {
            contracts.add(new Contract(entry.getKey()).parse(entry.getValue()));
        }
        calypsoCardCDMX.setContracts(contracts);
    }

}
