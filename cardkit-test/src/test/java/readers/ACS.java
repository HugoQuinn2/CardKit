package readers;

import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.NetworkCode;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.Profile;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Environment;
import com.idear.devices.card.cardkit.keyple.transaction.*;
import com.idear.devices.card.cardkit.keyple.transaction.essentials.EditCardFile;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.keyple.KeypleCalypsoSamReader;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.datamodel.ValueDecoder;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.junit.jupiter.api.Test;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.util.List;

public class ACS {

    public static final String BEA_ACS = "ACS ACR1281 1S Dual Reader PICC 0";
    public static final String SAM_ACS = "ACS ACR1281 1S Dual Reader SAM 0";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";
    public static final String aidCDMX = "315449432E494341D48401019101";

    @Test
    public void  editCard() throws Exception {
        KeypleCardReader reader = new KeypleCardReader(BEA_ACS, new KeypleCalypsoSamReader(SAM_ACS, lockSecret));
        reader.connect();
        reader.startApplicationSelection(Calypso.AID_CDMX);

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
        KeypleCardReader reader = new KeypleCardReader(BEA_ACS, new KeypleCalypsoSamReader(SAM_ACS, lockSecret));
        reader.connect();
        reader.startApplicationSelection(Calypso.AID_CDMX);
        LocationCode locationCode = new LocationCode(0xAABCDD);

        reader.getCardEventListenerList().add(transactionDataEvent -> System.out.println(transactionDataEvent.toJson()));
        reader.addListeners(event -> {
            if (!event.getType().equals(CardReaderEvent.Type.CARD_MATCHED))
                return;

            CalypsoCardCDMX calypsoCardCDMX =  reader.execute(new ReadAllCard())
                    .throwMessageOnAborted(CardException.class)
                    .throwMessageOnError(CardException.class)
                    .getData();

            Environment environment = Environment.buildEnvironment(NetworkCode.CDMX.getValue(), Profile.GENERAL.getValue());
            reader.execute(new PrePersonalization(PrePersonalization.KeyGenerated.LEGACY_SAM)).print();
        });

        safeWait(-1);
    }

    @Test
    public void property() {
        System.out.println(ValueDecoder.fromHexStringValue("CONTRALOR", Profile.class));
    }

    @Test
    public void frimware() throws javax.smartcardio.CardException {
        List<CardTerminal> terminals = TerminalFactory.getDefault().terminals().list();
        for (CardTerminal cardTerminal : terminals)
            System.out.println(cardTerminal);
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
