package readers;

import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.*;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.EditCardFile;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.ReadCardData;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SimpleReadCard;
import com.idear.devices.card.cardkit.core.datamodel.calypso.ContractStatus;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Provider;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.CalypsoSam;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ACS {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void renewContract() throws Exception {

        int daysOffset = 15;
        int locationId = 1118481;
        Provider provider = Provider.TREN_LIGERO;

        ReaderPCSC reader = new ReaderPCSC(
                BEA_ACS,
                new CalypsoSam(SAM_ACS, lockSecret)
        );

        reader.init();
        reader.initCardObserver(aidCDMX);
        reader.getCardEventListenerList().add(System.out::println);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(
                            new ReadCardData(WriteAccessLevel.DEBIT,
                                    reader.execute(new SimpleReadCard()).getData()))
                    .getData();
            Contract contract = calypsoCardCDMX.getContracts().findFirst(c -> c.getProvider().equals(provider)).get();

            // Expire contract
            contract.setDuration(0);
            reader.execute(new EditCardFile(contract, 1, WriteAccessLevel.LOAD));

            // Renew contract
//            System.out.println(reader.execute(new RenewedContract(calypsoCardCDMX, locationId, contract, daysOffset)).toJson());
//            System.out.println(reader.execute(
//                            new ReadCardData(WriteAccessLevel.DEBIT,
//                                    reader.execute(new SimpleReadCard()).getData()))
//                    .toJson());
        });

        Thread.sleep(10000);
    }

    enum Action {
        RELOAD,
        SELL,
        DEBIT,
        INITIALIZE,
    }

    @Test
    public void work() throws Exception {

        Action action = Action.RELOAD;
        int locationId = 1118481;

        Provider providerDevice = Provider.CABLEBUS;

        // Reload params
        AtomicInteger amount = new AtomicInteger(10_000);
        int maxAmount = 500_000;


        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.getCardEventListenerList().add(System.out::println);

        reader.addListeners(event -> {
            // Just work with card matched
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            // 1. First verification
            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCard()).getData();
            Contract contract = calypsoCardCDMX.getContracts().findFirst(
                    c -> c.getStatus().equals(ContractStatus.CONTRACT_PARTLY_USED)
            ).get();

            if (contract.isExpired(15))
                reader.execute(new RenewedCard(calypsoCardCDMX, locationId, contract, 15));


            TransactionResult<?> result = null;
            switch (action) {
                case RELOAD:
                    result = handlerReload(reader, calypsoCardCDMX, contract, locationId, amount.get(), maxAmount);
                    break;
                case DEBIT:
                    result = handlerDebit(reader, calypsoCardCDMX, contract, locationId, amount.get());
                    break;
                case SELL:
                    break;
                case INITIALIZE:
                    break;
            }

            System.out.println(result);
//            reader.execute(new ReadAllCard()).print();
        });

        safeWait(-1);
    }

    private TransactionResult<?> handlerReload(
            ReaderPCSC readerPCSC,
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int locationId,
            int amount,
            int maxAmount) {
        return readerPCSC.execute(new ReloadCard(calypsoCardCDMX, amount, contract, locationId).maxBalance(maxAmount));
    }

    private TransactionResult<?> handlerDebit(
            ReaderPCSC readerPCSC,
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int locationId,
            int amount) {
        return readerPCSC.execute(new DebitCard(calypsoCardCDMX, contract, amount, locationId));
    }

    private boolean isCardOnBlackList(String serial) {
        return false;
    }

    private boolean isSamOnWhiteList(String serial) {
        return true;
    }

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

    @Test
    public void toEmptyBalance() throws Exception {
        int daysOffset = 15;
        int locationId = 1118481;
        Provider provider = Provider.TREN_LIGERO;

        ReaderPCSC reader = new ReaderPCSC(
                BEA_ACS,
                new CalypsoSam(SAM_ACS, lockSecret)
        );

        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCard()).print().getData();
            Contract contract = calypsoCardCDMX.getContracts().findFirst(c -> c.getStatus().equals(ContractStatus.CONTRACT_PARTLY_USED)).get();

            System.out.println(reader.execute(new BalanceCancellation(
                    calypsoCardCDMX,
                    locationId,
                    contract,
                    TransactionType.BALANCE_CANCELLATION_DEBIT_SAM_NOT_WHITELISTED
            )));


            System.out.println(reader.execute(
                    new ReadCardData(
                            WriteAccessLevel.DEBIT,
                            reader.execute(
                                    new SimpleReadCard()).getData())));
        });

        safeWait(-1);
    }

}
