package readers;

import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.calypso.transaction.*;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.EditCardFile;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.location.StationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.CalypsoSam;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ACS {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void  editCard() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCard()).print().getData();
            calypsoCardCDMX.getEnvironment().setProfile(Profile.CONTRALOR);
            calypsoCardCDMX.getContracts().findFirst(c -> c.getStatus().isAccepted()).get().setDuration(PeriodType.encode(PeriodType.MONTH, 60));

            reader.execute(new EditCardFile(calypsoCardCDMX.getContracts().findFirst(c -> c.getStatus().isAccepted()).get(), 1, WriteAccessLevel.DEBIT)).print();
        });

        safeWait(-1);
    }

    @Test
    public void read() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            reader.execute(new ReadAllCard()).print();
        });

        safeWait(-1);
    }

    public static boolean isLastDebitSamOnWhiteList(CalypsoCardCDMX calypsoCardCDMX) {
        return true;
    }

    public static boolean isLastLoadSamOnWhiteList(CalypsoCardCDMX calypsoCardCDMX) {
        return true;
    }

    public static void reportEventToCentral(Event event) {
        System.out.println(event.toJson());
    }

    @Test
    public void debit() throws Exception {
        // Basic debit device params
        StationCode locationCode = new StationCode(1111111);
        Provider deviceProvider = Provider.CABLEBUS;
        int amount = 10_000;

        // Start reader with AID calypso card and sam lock secret
        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        // Add listener to cards events, this most be reported to central system
        reader.getCardEventListenerList().add(ACS::reportEventToCentral);

        // Add listener to readers events
        reader.addListeners(event -> {
            // Just work with CARD_MATCHED, this matched with que AID previously declared
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            // Read all card data and
            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCard()).getData();
            if (calypsoCardCDMX == null)
                return;

            // If the last load or debit sam is out, cancel the card balance
            if (!isLastLoadSamOnWhiteList(calypsoCardCDMX)) {
                reader.execute(
                        new BalanceCancellation(
                                calypsoCardCDMX,
                                locationCode.getCode(),
                                TransactionType.BALANCE_CANCELLATION_LOAD_SAM_NOT_WHITELISTED
                        ));
                return;
            } else if (!isLastDebitSamOnWhiteList(calypsoCardCDMX)){
                reader.execute(
                        new BalanceCancellation(
                                calypsoCardCDMX,
                                locationCode.getCode(),
                                TransactionType.BALANCE_CANCELLATION_DEBIT_SAM_NOT_WHITELISTED
                        ));
                return;
            }

            // Make debit
            TransactionResult<Boolean> debit = reader.execute(
                    new DebitCard(
                            calypsoCardCDMX,
                            deviceProvider,
                            locationCode,
                            amount)
            );

            // Save debit if was correct
        });

        safeWait(-1);
    }

    @Test
    public void reload() throws Exception {
        StationCode LOCATION_CODE = new StationCode(1, 2, Equipment.ANY_VALIDATOR, 1);
        int DEBIT_AMOUNT = 10_000;
        Provider DEVICE_PROVIDER = Provider.CABLEBUS;

        Executor executor = Executors.newSingleThreadExecutor();

        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            // Read card data
            TransactionResult<CalypsoCardCDMX> calypsoCardCDMXTransactionResult = reader.execute(new ReadAllCard());
            if (!calypsoCardCDMXTransactionResult.isOk())
                throw new ReaderException(calypsoCardCDMXTransactionResult.getMessage());

            // Make debit
            TransactionResult<Boolean> debit = reader.execute(
                    new ReloadCard(
                            calypsoCardCDMXTransactionResult.getData(),
                            100_000,
                            calypsoCardCDMXTransactionResult.getData().getContracts()
                                    .findFirst(c -> c.getStatus().isAccepted())
                                    .orElseThrow(() -> new CardException(
                                            "card '%s' without valid contract", calypsoCardCDMXTransactionResult.getData().getSerial())),
                            0,
                            LOCATION_CODE.getCode())
            );

            // Parse debit result on executor
            executor.execute(() -> System.out.println(debit.getMessage()));

            // Save debit if was correct
        });

        safeWait(-1);
    }

    @Test
    public void invalidate() throws Exception {
        StationCode LOCATION_CODE = new StationCode(1, 2, Equipment.ANY_VALIDATOR, 1);
        int DEBIT_AMOUNT = 10_000;
        Provider DEVICE_PROVIDER = Provider.CABLEBUS;

        Executor executor = Executors.newSingleThreadExecutor();

        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            // Read card data
            TransactionResult<CalypsoCardCDMX> calypsoCardCDMXTransactionResult = reader.execute(new ReadAllCard());
            if (!calypsoCardCDMXTransactionResult.isOk())
                throw new ReaderException(calypsoCardCDMXTransactionResult.getMessage());

            reader.execute(new InvalidateCard(
                    calypsoCardCDMXTransactionResult.getData(),
                    LOCATION_CODE.getCode()
            )).print();

            reader.execute(new ReadAllCard()).print();
        });

        safeWait(-1);
    }


    // Utils
    public static void safeWait(long time) {
        try {
            if (time < 0) {
                while (true) {
                    Thread.sleep(10);
                }
            } else {
                Thread.sleep(time);
            }
        } catch (InterruptedException e) {
            // just nothing
        }
    }
}
