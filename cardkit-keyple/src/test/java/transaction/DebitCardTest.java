package transaction;

import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.keyple.KeypleCalypsoSamReader;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Provider;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.keyple.transaction.PrePersonalization;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class DebitCardTest {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";

    public static final Provider provider = Provider.CABLEBUS;
    public static final LocationCode locationCode = new LocationCode(0xAAAAAA);

//    @Test
//    public void simpleDebitCard() throws Exception {
//        // Required debit card params
//        int amount = 1_000;
//        AtomicInteger passenger = new AtomicInteger();
//
//        // Start card and sam reader with auto select application AID
//        KeypleCardReader keypleCardReader = new KeypleCardReader(BEA_ACS, new KeypleCalypsoSamReader(SAM_ACS, lockSecret));
//        keypleCardReader.connect();
//        keypleCardReader.startApplicationSelection(Calypso.AID_CDMX);
//
//        // Deffine
//        keypleCardReader.addListeners(event -> {
//            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
//                return;
//
//            CalypsoCardCDMX calypsoCardCDMX =  keypleCardReader.factory().readAllCard().print().getData();
//            TransactionResult<?> debitTransactionResult = keypleCardReader.factory().debitCard(
//                    calypsoCardCDMX, provider.getValue(), locationCode.getValue(), amount, passenger.get()
//            );
//
//            if (debitTransactionResult.isOk())
//                System.out.println("Debit successfully, pass away");
//
//            System.out.println(debitTransactionResult.getMessage());
//        });
//
//        safeWait(-1);
//    }
//
//    @Test
//    public void prePersonalization() throws Exception {
//        // Start card and sam reader with auto select application AID
//        KeypleCalypsoSamReader keypleCalypsoSamReader = new KeypleCalypsoSamReader(SAM_ACS, lockSecret);
//        KeypleCardReader keypleCardReader = new KeypleCardReader(BEA_ACS, keypleCalypsoSamReader);
//        keypleCardReader.connect();
//        keypleCardReader.startApplicationSelection(Calypso.AID_CDMX);
//
//        keypleCardReader.addListeners(event -> {
//            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
//                return;
//
//            keypleCardReader.execute(new PrePersonalization(PrePersonalization.KeyGenerated.LEGACY_SAM)).print();
//
//        });
//
//        safeWait(-1);
//    }

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
