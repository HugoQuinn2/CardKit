package readers;

import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.EditCardFile;
import com.idear.devices.card.cardkit.calypso.transaction.RenewedContract;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.CalypsoSam;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.transaction.ReadAllCardData;
import com.idear.devices.card.cardkit.calypso.transaction.SimpleReadCard;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.junit.jupiter.api.Test;

public class ACS {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void simpleCalypso() throws Exception {

        ReaderPCSC reader = new ReaderPCSC(
                BEA_ACS,
                new CalypsoSam(SAM_ACS, lockSecret)
        );

        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            TransactionResult<CalypsoCardCDMX> readResult;

            if (event.getType().equals(CardReaderEvent.Type.CARD_MATCHED)) {
                readResult = reader.execute(new SimpleReadCard());

                if (readResult.isOk()) {
                    readResult = reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT));
                    if (readResult.isOk())
                        System.out.println(readResult.getData().getContracts().get(0).toJson());
                    else
                        System.out.println(readResult.toJson());
                }
            }
        });

        Thread.sleep(10000);
    }

    @Test
    public void renewContract() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(
                BEA_ACS,
                new CalypsoSam(SAM_ACS, lockSecret)
        );

        reader.init();
        reader.initCardObserver(aidCDMX);

        reader.addListeners(event -> {
            TransactionResult<CalypsoCardCDMX> readResult;

            if (event.getType().equals(CardReaderEvent.Type.CARD_MATCHED)) {
                readResult = reader.execute(new SimpleReadCard());

                if (readResult.isOk()) {
                    readResult = reader.execute(new ReadAllCardData(WriteAccessLevel.DEBIT));
                    if (readResult.isOk()) {
                        Contract contract = readResult.getData().getContracts().get(0);

                        contract.setDuration(0);
                        reader.execute(new EditCardFile(contract, 1));


                        TransactionResult<Boolean> renew = reader.execute(new RenewedContract(contract, 15));
                        System.out.println(renew.toJson());
                    }
                    else
                        System.out.println(readResult.toJson());
                }
            }
        });

        Thread.sleep(10000);
    }

}
