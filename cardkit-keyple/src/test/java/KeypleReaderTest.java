import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.Contract;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.keyple.KeypleCalypsoSamReader;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionManager;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.junit.jupiter.api.Test;

import javax.smartcardio.CommandAPDU;

public class KeypleReaderTest {
    public static final String ACS_CARD_READER = ".*ACS ACR1281 1S Dual Reader PICC.*";
    public static final String ACS_SAM_READER = ".*ACS ACR1281 1S Dual Reader SAM.*";
    public static final String lockSecret = "A40B01C39C99CB910FE62A23192A0C5C";

    @Test
    public void basicConnect() throws Exception {
        KeypleCalypsoSamReader kcsr = new KeypleCalypsoSamReader(ACS_SAM_READER, lockSecret);
        KeypleCardReader kcr = new KeypleCardReader(ACS_CARD_READER, Calypso.AID_CDMX);

        kcr.connect();
        kcsr.connect();

        kcr.waitForCardPresent(0);

        KeypleTransactionManager ktm = new KeypleTransactionManager(kcr, kcsr, Calypso.AID_CDMX);
        CalypsoCardCDMX calypsoCardCDMX = ktm.readCardData().getData();
//        ktm.purchaseCard(calypsoCardCDMX, calypsoCardCDMX.getContracts().getFirstContractValid(), 0xAAAAAA, Provider.CABLEBUS.getValue(), 0, 0).print();
        ktm.renewContract(calypsoCardCDMX, 0xAAAAAA, Provider.CABLEBUS.getValue(), 0, ReverseDate.now(), 60);
//        ktm.balanceCancellation(calypsoCardCDMX, TransactionType.BALANCE_CANCELLATION_DEBIT_SAM_NOT_WHITELISTED.getValue(), 0xAAAAAA, Provider.CABLEBUS.getValue(), 0).print();
//        ktm.rehabilitateCard(calypsoCardCDMX).print();
//        ktm.invalidateCard(calypsoCardCDMX, TransactionType.INVALIDATION.getValue(), 0xAAAAAA, Provider.CABLEBUS.getValue(), 0).print();
//        ktm.debitCard(calypsoCardCDMX, 0xAAAAAA, Provider.CABLEBUS.getValue(), 0, 1_000).print();
//        ktm.reloadCard(calypsoCardCDMX, 0xAAAAAA, Provider.CABLEBUS.getValue(), 0, 500_00).print();

        ktm.readCardData().print();
    }
}
