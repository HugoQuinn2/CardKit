package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.keyple.KeypleCalypsoSamReader;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
public class PrePersonalization extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {
        return null;
    }

    public enum KeyGenerated {
        CARD,
        LEGACY_SAM
    }

    private final KeyGenerated keyGenerate;
    private final LocalDate startDate;
    private final LocalDate endDate;
//
//    @Override
//    public TransactionResult<Boolean> execute(KeypleCardReader reader) {
//        CalypsoCard calypsoCard = reader.getCalypsoCard();
//        log.info("Pre personalization card {} with {}, from {} to {}",
//                HexUtil.toHex(calypsoCard.getApplicationSerialNumber()),
//                keyGenerate,
//                startDate,
//                endDate);
//
//        LegacySam legacySam = selectLegacySam(reader.getKeypleCalypsoSamReader().getSamReader());
//        switch (keyGenerate) {
//            case CARD:
//                cardSamKeyPair(reader, legacySam, startDate, endDate);
//            break;
//            case LEGACY_SAM:
//                legacySamKeyPair(reader, legacySam, startDate, endDate);
//                break;
//        }
//
//        return TransactionResult.<Boolean>builder()
//                .transactionStatus(TransactionStatus.OK)
//                .data(true)
//                .build();
//    }
//
//    private void cardSamKeyPair(KeypleCardReader reader, LegacySam legacySam, LocalDate startDate, LocalDate endDate) {
//        CalypsoCard calypsoCard = reader.getCalypsoCard();
//
//        try {
//            reader.getFreeTransactionManager()
//                    .prepareGenerateAsymmetricKeyPair()
//                    .prepareGetData(org.eclipse.keypop.calypso.card.GetDataTag.CARD_PUBLIC_KEY)
//                    .processCommands(ChannelControl.KEEP_OPEN);
//        } catch (Exception e) {
//            throw new CardException("Error making ECC key pair: " + e.getMessage());
//        }
//
//        LegacyCardCertificateComputationData cardCertificateComputationData =
//                KeypleCalypsoSamReader.legacySamExtensionService.getLegacySamApiFactory()
//                        .createLegacyCardCertificateComputationData()
//                        .setCardAid(calypsoCard.getDfName())
//                        .setCardSerialNumber(calypsoCard.getApplicationSerialNumber())
//                        .setStartDate(startDate)
//                        .setEndDate(endDate)
//                        .setCardStartupInfo(calypsoCard.getStartupInfoRawData());
//
//        try {
//            KeypleCalypsoSamReader.legacySamExtensionService.getLegacySamApiFactory()
//                    .createFreeTransactionManager(reader.getKeypleCalypsoSamReader().getSamReader(), legacySam)
//                    .prepareGetData(GetDataTag.CA_CERTIFICATE)
//                    .prepareComputeCardCertificate(cardCertificateComputationData)
//                    .processCommands();
//        } catch (Exception e) {
//            throw new SamException("Error generating PKI sam certification: " + e.getMessage());
//        }
//
//        try {
//            reader.getFreeTransactionManager()
//                    .preparePutData(PutDataTag.CA_CERTIFICATE, legacySam.getCaCertificate())
//                    .preparePutData(PutDataTag.CARD_CERTIFICATE, cardCertificateComputationData.getCertificate())
//                    .processCommands(ChannelControl.CLOSE_AFTER);
//        } catch (Exception e) {
//            throw new CardException("Error putting sam certification data on card: " + e.getMessage());
//        }
//
//    }
//
//    private void legacySamKeyPair(KeypleCardReader reader, LegacySam legacySam, LocalDate startDate, LocalDate endDate) {
//        KeyPairContainer keyPairContainer = KeypleCalypsoSamReader.legacySamExtensionService.getLegacySamApiFactory().createKeyPairContainer();
//        CalypsoCard calypsoCard = reader.getCalypsoCard();
//
//        LegacyCardCertificateComputationData cardCertificateComputationData =
//                KeypleCalypsoSamReader.legacySamExtensionService.getLegacySamApiFactory()
//                        .createLegacyCardCertificateComputationData()
//                        .setCardAid(calypsoCard.getDfName())
//                        .setCardSerialNumber(calypsoCard.getApplicationSerialNumber())
//                        .setStartDate(startDate)
//                        .setEndDate(endDate)
//                        .setCardStartupInfo(calypsoCard.getStartupInfoRawData());
//
//        try {
//            KeypleCalypsoSamReader.legacySamExtensionService.getLegacySamApiFactory()
//                    .createFreeTransactionManager(reader.getKeypleCalypsoSamReader().getSamReader(), legacySam)
//                    .prepareGetData(GetDataTag.CA_CERTIFICATE)
//                    .prepareGenerateCardAsymmetricKeyPair(keyPairContainer)
//                    .prepareComputeCardCertificate(cardCertificateComputationData)
//                    .processCommands();
//        } catch (Exception e) {
//            throw new SamException("Error generating PKI sam certification: " + e.getMessage());
//        }
//
//        try {
//            reader.getFreeTransactionManager()
//                    .preparePutData(PutDataTag.CA_CERTIFICATE, legacySam.getCaCertificate())
//                    .preparePutData(PutDataTag.CARD_KEY_PAIR, keyPairContainer.getKeyPair())
//                    .preparePutData(PutDataTag.CA_CERTIFICATE, cardCertificateComputationData.getCertificate())
//                    .processCommands(ChannelControl.KEEP_OPEN);
//        }catch (Exception e) {
//            throw new CardException("Error putting sam certification data on card: " + e.getMessage());
//        }
//    }
//
//    private LegacySam selectLegacySam(CardReader cardReader) {
//        // Create a SAM selection manager.
//        CardSelectionManager samSelectionManager = KeypleUtil.READER_API_FACTORY.createCardSelectionManager();
//
//        // Create a card selector without filer
//        IsoCardSelector cardSelector =
//                KeypleUtil.READER_API_FACTORY
//                        .createIsoCardSelector()
//                        .filterByPowerOnData(
//                                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));
//
//        LegacySamApiFactory legacySamApiFactory =
//                LegacySamExtensionService.getInstance().getLegacySamApiFactory();
//
//        // Create a SAM selection using the Calypso card extension.
//        samSelectionManager.prepareSelection(
//                cardSelector, legacySamApiFactory.createLegacySamSelectionExtension());
//
//        // SAM communication: run the selection scenario.
//        CardSelectionResult samSelectionResult =
//                samSelectionManager.processCardSelectionScenario(cardReader);
//
//        // Check the selection result.
//        if (samSelectionResult.getActiveSmartCard() == null) {
//            throw new SamException("The selection of the SAM failed, mode SAM_C1 required.");
//        }
//
//        // Get the Calypso SAM SmartCard resulting of the selection.
//        return (LegacySam) samSelectionResult.getActiveSmartCard();
//    }
}
