package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.file.*;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.datamodel.date.DateTimeReal;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.reader.GenericApduResponse;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.generic.CardTransactionManager;
import org.eclipse.keyple.card.generic.TransactionException;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.card.ElementaryFile;
import org.eclipse.keypop.calypso.card.transaction.*;
import org.eclipse.keypop.calypso.card.transaction.spi.SymmetricCryptoCardTransactionManagerFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySamSelectionExtension;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.CardTransactionLegacySamExtension;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.SamTraceabilityMode;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.TraceableSignatureComputationData;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.spi.SmartCard;

import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;

import static com.idear.devices.card.cardkit.keyple.KeypleCardReader.calypsoCardApiFactory;

public abstract class KeypleUtil {

    public static final SmartCardService SMART_CARD_SERVICE = SmartCardServiceProvider.getService();
    public static final ReaderApiFactory READER_API_FACTORY = SMART_CARD_SERVICE.getReaderApiFactory();
    public static final Plugin PLUGIN = SMART_CARD_SERVICE.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    public static CardReader getCardReaderMatchingName(
            String matchName) {
        Set<String> setReaderNames = PLUGIN.getReaderNames();

        if (setReaderNames.isEmpty())
            throw new RuntimeException("no readers available");

        for (String readerNames : setReaderNames)
            if (readerNames.matches(matchName))
                return PLUGIN.getReader(readerNames);

        throw new RuntimeException(setReaderNames.size() + " card readers found, none matched the pattern");
    }

    public static CalypsoCard selectCard(
            CardReader cardReader,
            String aid) {

        CardSelectionManager cardSelectionManager = READER_API_FACTORY.createCardSelectionManager();
        CalypsoCardSelectionExtension calypsoCardSelection = calypsoCardApiFactory
                .createCalypsoCardSelectionExtension()
                .acceptInvalidatedCard();

        cardSelectionManager.prepareSelection(
                READER_API_FACTORY.createIsoCardSelector()
                        .filterByDfName(aid),
                calypsoCardSelection);

        SmartCard smartCard;

        smartCard = cardSelectionManager
                .processCardSelectionScenario(cardReader)
                .getActiveSmartCard();

        if (smartCard == null)
            throw new IllegalStateException("The selection of the application " + aid + " failed.");

        return  (CalypsoCard) smartCard;
    }

    public static LegacySam selectAndUnlockSam(
            CardReader samReader,
            String lockSecret) {
        // Get reader api factory
        ReaderApiFactory readerApiFactory = SmartCardServiceProvider.getService().getReaderApiFactory();

        // Create a SAM selection manager.
        CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

        // Create a card selector without filter
        CardSelector<BasicCardSelector> samCardSelector = readerApiFactory.createBasicCardSelector();

        // Get legacy sam api factory
        LegacySamApiFactory samApiFactory =
                LegacySamExtensionService.getInstance().getLegacySamApiFactory();

        // Create a SAM selection
        LegacySamSelectionExtension samSelection = samApiFactory
                .createLegacySamSelectionExtension()
                .prepareReadAllCountersStatus();

        // Set lock secret
        if (lockSecret != null)
            samSelection.setUnlockData(lockSecret);

        // Prepare SAM selection
        samSelectionManager.prepareSelection(samCardSelector, samSelection);

        // Get SAM
        LegacySam sam = null;
        try {
            sam = (LegacySam) samSelectionManager
                    .processCardSelectionScenario(samReader)
                    .getActiveSmartCard();
        } catch (Exception ex) {
            throw new SamException("Calypso SAM selection failed in reader %s.", samReader.getName());
        }

        if (sam == null)
            throw new SamException("No Calypso SAM in reader %s.", samReader.getName());

        return sam;
    }

    public static SymmetricCryptoSecuritySetting startSymmetricSecuritySettings(
            CardReader samReader,
            LegacySam sam) {
        SymmetricCryptoCardTransactionManagerFactory symmetricCryptoCardTransactionManagerFactory =
                LegacySamExtensionService.getInstance().getLegacySamApiFactory()
                        .createSymmetricCryptoCardTransactionManagerFactory(
                                samReader, sam);

        CalypsoCardApiFactory calypsoApiFactory = CalypsoExtensionService.getInstance()
                .getCalypsoCardApiFactory();

        return calypsoApiFactory
                .createSymmetricCryptoSecuritySetting(symmetricCryptoCardTransactionManagerFactory)
                .enableSvLoadAndDebitLog()
                .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, (byte) 0x21)
                .assignDefaultKif(WriteAccessLevel.LOAD, (byte) 0x27)
                .assignDefaultKif(WriteAccessLevel.DEBIT, (byte) 0x30);
    }

    public static byte[] readCardFile(
            SecureRegularModeTransactionManager ctm,
            CalypsoCard calypsoCard,
            WriteAccessLevel writeAccessLevel,
            byte fileId,
            int record) {
        try {
            ctm
                    .prepareOpenSecureSession(writeAccessLevel)
                    .prepareReadRecord(fileId, record)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);
        } catch (Exception e) {
            throw new CardException(e.getMessage());
        }

        ElementaryFile elementaryFile = calypsoCard.getFileBySfi(fileId);

        if (elementaryFile == null)
            throw new CardException("The file %s could not be read", fileId);

        return elementaryFile.getData().getContent();
    }

    public static SortedMap<Integer, byte[]> readCardPartially(
            SecureRegularModeTransactionManager ctm,
            CalypsoCard calypsoCard,
            WriteAccessLevel writeAccessLevel,
            byte fileId,
            int fromRecord,
            int toRecord,
            int offset,
            int bytesToRead) {
        try {
            ctm
                    .prepareOpenSecureSession(writeAccessLevel)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareReadRecordsPartially(
                            fileId,
                            fromRecord,
                            toRecord,
                            offset,
                            bytesToRead)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);
        } catch (Exception e) {
            throw new CardException(e.getMessage());
        }

        return calypsoCard.getFileBySfi(fileId)
                .getData()
                .getAllRecordsContent();
    }

    public static void appendEditCardFile(
            SecureRegularModeTransactionManager ctm,
            WriteAccessLevel writeAccessLevel,
            byte fileId,
            byte[] fileData,
            ChannelControl channelControl) {
        try {
            ctm
                    .prepareOpenSecureSession(writeAccessLevel)
                    .prepareAppendRecord(
                            fileId,
                            fileData)
                    .prepareCloseSecureSession()
                    .processCommands(channelControl);
        } catch (Exception e) {
            throw new CardException(e.getMessage());
        }
    }

    public static void appendEditCardFile(
            SecureRegularModeTransactionManager ctm,
            WriteAccessLevel writeAccessLevel,
            byte fileId,
            byte[] fileData) {
        appendEditCardFile(ctm, writeAccessLevel, fileId, fileData, ChannelControl.KEEP_OPEN);
    }

    public static void editCardFile(
            SecureRegularModeTransactionManager ctm,
            WriteAccessLevel writeAccessLevel,
            byte fileId,
            int record,
            byte[] data,
            ChannelControl channelControl) {
        try {
            ctm
                    .prepareOpenSecureSession(writeAccessLevel)
                    .prepareUpdateRecord(fileId, record, data)
                    .prepareCloseSecureSession()
                    .processCommands(channelControl);
        } catch (Exception e) {
            throw new CardException("Error updating file card: ", e.getMessage());
        }
    }

    public static void editCardFile(
            SecureRegularModeTransactionManager ctm,
            WriteAccessLevel writeAccessLevel,
            byte fileId,
            int record,
            byte[] data) {
        editCardFile(ctm, writeAccessLevel, fileId, record, data, ChannelControl.KEEP_OPEN);
    }


    public static TransactionDataEvent saveEvent(
            SecureRegularModeTransactionManager ctm,
            CalypsoCardCDMX calypsoCardCDMX,
            CalypsoCard calypsoCard,
            KeypleCalypsoSamReader keypleCalypsoSamReader,
            Event event,
            Contract contract,
            int initBalance,
            int provider) {
        String mac = "";
        TransactionType transactionType = event.getTransactionType().decode(TransactionType.RFU);
        if (transactionType.isSigned())
            mac = computeTransactionSignature(keypleCalypsoSamReader, event, calypsoCardCDMX, initBalance, provider);

        Logs logs = readCardLogs(ctm, calypsoCard);

        if (transactionType.isWritten())
            appendEditCardFile(ctm, WriteAccessLevel.LOAD, Calypso.EVENT_FILE, event.unparse(), ChannelControl.CLOSE_AFTER);
        else
            ctm.processCommands(ChannelControl.CLOSE_AFTER);

        return TransactionDataEvent
                .builder()
                .mac(mac)
                .debitLog(logs.getDebitLog())
                .loadLog(logs.getLoadLog())
                .event(event)
                .contract(contract)
                .profile(calypsoCardCDMX.getEnvironment().getProfile().getValue())
                .transactionAmount(event.getAmount())
                .balanceBeforeTransaction(initBalance)
                .locationCode(event.getLocationId())
                .build();
    }

    public static String computeTransactionSignature(
            KeypleCalypsoSamReader keypleCalypsoSamReader,
            int eventType,
            int transactionTimestamp,
            int transactionAmount,
            int terminalLocation,
            int cardType,
            String cardSerialHex,
            int prevSvBalance,
            int svProvider) {
        BitUtil bit = new BitUtil(0x20 * 8);
        bit.setNextInteger(eventType, 8);
        bit.setNextInteger(transactionTimestamp, 32);
        bit.setNextInteger(Math.abs(transactionAmount), 32);
        bit.setNextInteger(terminalLocation, 32);
        bit.setNextInteger(cardType, 8);
        bit.setNextHexaString(cardSerialHex, 64);
        bit.setNextInteger(prevSvBalance, 32);
        bit.setNextInteger(svProvider, 8);
        bit.setNextInteger(0, 16);
        bit.setNextInteger(0, 24);

        GenericApduResponse response = digestMacCompute(
                keypleCalypsoSamReader.getGenericSamTransactionManager(),
                (byte) 0xEB,
                (byte) 0xC0,
                bit.getData());

        return HexUtil.toHex(response.getDataOut());
    }

    public static String computeTransactionSignature(
            KeypleCalypsoSamReader keypleCalypsoSamReader,
            Event event,
            CalypsoCardCDMX calypsoCardCDMX,
            int prevSvBalance,
            int svProvider) {
        return computeTransactionSignature(
                keypleCalypsoSamReader,
                event.getTransactionType().getValue(),
                DateTimeReal.now().getValue(),
                event.getAmount(),
                event.getLocationId().getValue(),
                calypsoCardCDMX.getCalypsoProduct().getValue(),
                calypsoCardCDMX.getSerial(),
                prevSvBalance,
                svProvider
        );
    }

    public static GenericApduResponse digestMacCompute(
            CardTransactionManager samGenericTransactionManager,
            byte kif,
            byte kvc,
            byte[] data) {
        byte cla = (byte) 0x80;
        byte ins = (byte) 0x8F;
        byte p1  = (byte) 0x00;
        byte p2  = (byte) 0x00;
        byte lc  = (byte) 0x22;

        byte[] head = {cla, ins, p1, p2, lc, kif, kvc};
        byte[] apdu = new byte[head.length + data.length];
        System.arraycopy(head, 0, apdu, 0, head.length);
        System.arraycopy(data, 0, apdu, head.length, data.length);

        byte[] mac;
        String sw = "";
        try {
            byte[] response = samGenericTransactionManager
                    .prepareApdu(apdu)
                    .processApdusToByteArrays(org.eclipse.keyple.card.generic.ChannelControl.KEEP_OPEN)
                    .get(0);
            mac = Arrays.copyOfRange(response, 0, response.length - 2);
            sw = org.eclipse.keyple.core.util.HexUtil.toHex(Arrays.copyOfRange(
                    response, response.length-2, response.length));
        } catch (TransactionException e) {
            mac = new byte[0];
        }
        return new GenericApduResponse(mac, sw);
    }

    public static Logs readCardLogs(
            SecureRegularModeTransactionManager ctm,
            CalypsoCard calypsoCard) {
        ctm
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        Logs logs = new Logs();
        logs.setDebitLog(new DebitLog().parse(calypsoCard.getSvDebitLogLastRecord()));
        logs.setLoadLog(new LoadLog().parse(calypsoCard.getSvLoadLogRecord()));

        return logs;
    }

    public static void performDebit(
            SecureRegularModeTransactionManager ctm,
            int amount) {
        ctm
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                .prepareSvDebit(
                        amount,
                        CompactDate.now().toBytes(),
                        CompactTime.now().toBytes())
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);
    }

    public static void rehabilitateCard(
            SecureRegularModeTransactionManager ctm) {
        ctm
                .prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION)
                .prepareRehabilitate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);
    }

    public static void invalidateCard(
            SecureRegularModeTransactionManager ctm) {
        ctm
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareInvalidate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);
    }

    public static void reloadCard(
            SecureRegularModeTransactionManager ctm,
            int amount) {
        ctm
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareSvGet(SvOperation.RELOAD, SvAction.DO)
                .prepareSvReload(
                        amount,
                        CompactDate.now().toBytes(),
                        CompactTime.now().toBytes(),
                        ByteUtils.extractBytes(0, 2))
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);
    }

    public static void updateRecord(
            SecureRegularModeTransactionManager ctm,
            byte fileId,
            int recordNumber,
            byte[] data) {
        ctm
                .prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION)
                .prepareUpdateRecord(
                        fileId,
                        recordNumber,
                        data)
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);
    }

    public static void updateRecord(
            SecureRegularModeTransactionManager ctm,
            File<?> file,
            int recordNumber) {
        updateRecord(ctm, file.getFileId(), recordNumber, file.unparse());
    }

    public static TraceableSignatureComputationData buildSignatureComputationData(
            byte[] fullSerial,
            Contract contract) {
        return LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createTraceableSignatureComputationData()
                .withSamTraceabilityMode(
                        0xD0,
                        SamTraceabilityMode.FULL_SERIAL_NUMBER)
                .setSignatureSize(3)
                .setData(buildSignatureData(fullSerial, contract), (byte) 0x2C, (byte) 0xC4);
    }

    public static byte[] buildSignatureData(
            byte[] fullSerial,
            Contract contract) {
        byte[] signatureData = new byte[34];
        System.arraycopy(fullSerial, 0, signatureData, 0, 8);

        byte[] _contract = contract.unparse();
        System.arraycopy(_contract, 0, signatureData, 8, _contract.length - 3);

        signatureData[9] = 0; // ContractStatus
        signatureData[10] = 0; // ContractRfu and ContractValidityStartDate MSb
        signatureData[11] = 0; // ContractValidityStartDate LSb

        return signatureData;
    }

    public static Contract setupRenewContract(
            SecureRegularModeTransactionManager ctm,
            byte[] fullSerial,
            Contract contract,
            int provider,
            ReverseDate startDate,
            int duration) {

        Contract wEfContract = new Contract(contract.getId()).parse(contract.unparse());
        wEfContract.getVersion().setValue(Version.VERSION_3_3);
        wEfContract.setRfu(0);
        wEfContract.getStatus().setValue(0);
        wEfContract.setStartDate(ReverseDate.zero());
        wEfContract.setDuration(duration);
        wEfContract.getNetwork().setValue(NetworkCode.CDMX);
        wEfContract.getProvider().setValue(provider);
        wEfContract.getModality().setValue(Modality.MONOMODAL);
        wEfContract.getTariff().setValue(Tariff.STORED_VALUE);
        wEfContract.setJourneyInterChanges(1);
        wEfContract.setSaleDate(CompactDate.now());
        wEfContract.setAuthKvc((byte) 0xC4 & 0xff);

        // Compute contract signature
        TraceableSignatureComputationData signatureData = buildSignatureComputationData(
                fullSerial,
                wEfContract);
        ctm.getCryptoExtension(CardTransactionLegacySamExtension.class)
                .prepareComputeSignature(signatureData);
        ctm.processCommands(ChannelControl.KEEP_OPEN);

        // Get signed contract
        int signature = ByteArrayUtil.extractInt(
                signatureData.getSignature(),
                0, 3, false);

        byte[] signedContract = Arrays.copyOfRange(
                signatureData.getSignedData(),
                fullSerial.length, signatureData.getSignedData().length);

        wEfContract.parse(signedContract);

        // Set signature independent fields
        wEfContract.setRfu(0);
        wEfContract.getStatus().setValue(ContractStatus.CONTRACT_PARTLY_USED);
        wEfContract.setStartDate(startDate);
        wEfContract.setAuthenticator(signature);
        wEfContract.update();

        return wEfContract;
    }

    public static SecureRegularModeTransactionManager prepareCardTransactionManger(
            CardReader cardReader,
            CalypsoCard calypsoCard,
            SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting) {
        return CalypsoExtensionService.getInstance()
                .getCalypsoCardApiFactory()
                .createSecureRegularModeTransactionManager(
                        cardReader,
                        calypsoCard,
                        symmetricCryptoSecuritySetting
                );
    }

}
