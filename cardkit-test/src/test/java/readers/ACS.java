package readers;

import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.calypso.transaction.*;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.EditCardFile;
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

public class ACS {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void read() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(
                BEA_ACS,
                new CalypsoSam(SAM_ACS, lockSecret)
        );

        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            System.out.println(
                    reader.execute(new SimpleReadCard()).toJson()
            );
        });

        Thread.sleep(10000);
    }

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

        reader.addListeners(event -> {

            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).getData();
            Contract contract = calypsoCardCDMX.getContracts().findFirst(c -> c.getProvider().equals(provider)).get();

            // Expire contract
            contract.setDuration(0);
            reader.execute(new EditCardFile(contract, 1, WriteAccessLevel.LOAD));

            // Renew contract
            System.out.println(reader.execute(new RenewedContract(calypsoCardCDMX, locationId, contract, daysOffset)).toJson());
            System.out.println(reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).toJson());
        });

        Thread.sleep(10000);
    }

    @Test
    public void reload() throws Exception {

        // Required params for reload
        int amount = 100;
        Provider provider = Provider.TREN_LIGERO;

        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {

            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).getData();

            System.out.println(
                    reader.execute(
                            new ReloadAndRenewCard(
                                    reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).getData(),
                                    amount,
                                    calypsoCardCDMX.getContracts().findFirst(c -> c.getStatus().equals(ContractStatus.CONTRACT_PARTLY_USED)).get(),
                                    1118481)
                                    .provider(provider.getValue())
                    ).toJson()
            );

        });

        Thread.sleep(10000);
    }

    @Test
    public void event() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(
                BEA_ACS,
                new CalypsoSam(SAM_ACS, lockSecret)
        );

        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).getData();
            Event e = Event.builEvent(
                    TransactionType.RELOAD,
                    reader.getCalypsoSam(),
                    calypsoCardCDMX.getEvents().getLast().getTransactionNumber() + 1,
                    1118481,
                    1
            );

            calypsoCardCDMX.getEvents().append(e);
            System.out.println(
                    reader.execute(
                            new EditCardFile(
                                    calypsoCardCDMX.getEvents(),
                                    calypsoCardCDMX.getEvents().size(),
                                    WriteAccessLevel.LOAD))
                            .toJson()
            );

            System.out.println(reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).toJson());
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
        int amount = 100;
        int maxAmount = 500_000;


        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            // Just work with card matched
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            // Read card and find a valid contract
            CalypsoCardCDMX calypsoCardCDMX = reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT)).getData();
            Optional<Contract> optionalContract = calypsoCardCDMX.getContracts().findFirst(
                    c ->
                            !c.getProvider().equals(Provider.RFU) &&
                                    !c.getStatus().equals(ContractStatus.CONTRACT_NEVER_USED)
            );

            if (!optionalContract.isPresent())
                throw new CardException("No valid contract for provider ");

            Contract contract = optionalContract.get();

            TransactionResult<?> result = null;
            switch (action) {
                case RELOAD:
                    result = handlerReload(reader, calypsoCardCDMX, contract, locationId, amount, maxAmount);
                case SELL:
                    break;
                case INITIALIZE:
                    break;
            }

            System.out.println(result.toJson());
        });

        Thread.sleep(10000);
    }

    private TransactionResult<?> handlerReload(
            ReaderPCSC readerPCSC,
            CalypsoCardCDMX calypsoCardCDMX,
            Contract contract,
            int locationId,
            int amount,
            int maxAmount) {
        return readerPCSC.execute(new ReloadAndRenewCard(calypsoCardCDMX, amount, contract, locationId).maxBalance(maxAmount));
    }

}
