package readers;

import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.file.Environment;
import com.idear.devices.card.cardkit.calypso.transaction.*;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.EditCardFile;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.CalypsoSam;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.datamodel.ValueDecoder;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.junit.jupiter.api.Test;

public class ACS {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void  editCard() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(Calypso.AID_CDMX);

        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX = reader.execute(
                            new ReadAllCard())
                    .throwMessageOnError(CardException.class)
                    .throwMessageOnAborted(CardException.class)
                    .getData();

            calypsoCardCDMX.getEnvironment().getProfile().setValue(Profile.GENERAL);

            reader.execute(
                    new EditCardFile(
                            calypsoCardCDMX.getEnvironment(),
                            1,
                            WriteAccessLevel.PERSONALIZATION))
                    .print();

            reader.execute(
                            new ReadAllCard())
                    .throwMessageOnError(CardException.class)
                    .throwMessageOnAborted(CardException.class)
                    .print()
                    .getData();
        });

        safeWait(-1);
    }

    @Test
    public void read() throws Exception {
        ReaderPCSC reader = new ReaderPCSC(BEA_ACS, new CalypsoSam(SAM_ACS, lockSecret));
        reader.init();
        reader.initCardObserver(Calypso.AID_CDMX);
        LocationCode locationCode = new LocationCode(0xAABCDD);

        reader.getCardEventListenerList().add(transactionDataEvent -> System.out.println(transactionDataEvent.toJson()));
        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX =  reader.execute(new ReadAllCard())
                    .throwMessageOnAborted(CardException.class)
                    .throwMessageOnError(CardException.class)
                    .print()
                    .getData();

            Environment environment = new Environment()
                    .parse(HexUtil.toByteArray("1484015A00000000275A2F1500000000000000000002F1500000000000"));

            reader.getCalypsoSam().

            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareWriteBinary(
                            environment.getFileId(),
                            0,
                            environment.unparse())
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);

//            reader.execute(new DebitCard(
//                    calypsoCardCDMX,
//                    Provider.CABLEBUS,
//                    locationCode,
//                    1_000
//            ));
        });

        safeWait(-1);
    }

    @Test
    public void property() {
        System.out.println(ValueDecoder.fromHexStringValue("CONTRALOR", Profile.class));
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
