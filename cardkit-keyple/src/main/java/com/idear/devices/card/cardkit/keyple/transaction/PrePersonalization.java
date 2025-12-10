package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.keyple.KeypleCalypsoSam;
import com.idear.devices.card.cardkit.keyple.KeypleReader;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.PutDataTag;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.crypto.legacysam.GetDataTag;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.KeyPairContainer;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.LegacyCardCertificateComputationData;

import java.time.LocalDate;

@Slf4j
public class PrePersonalization extends Transaction<Boolean, KeypleReader> {

    public enum KeyGenerated {
        CARD,
        LEGACY_SAM
    }

    private final KeyGenerated keyGenerate;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public PrePersonalization(
            KeyGenerated keyGenerate) {
        this(keyGenerate, LocalDate.now());
    }

    public PrePersonalization(
            KeyGenerated keyGenerate,
            LocalDate startDate) {
        this(keyGenerate, startDate, startDate.plusYears(5).minusDays(5));
    }

    public PrePersonalization(
            KeyGenerated keyGenerate,
            LocalDate startDate,
            LocalDate endDate) {
        super("PRE_PERSONALIZATION");
        this.keyGenerate = keyGenerate;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public TransactionResult<Boolean> execute(KeypleReader reader) {
        CalypsoCard calypsoCard = reader.getCalypsoCard();
        LegacySam legacySam = reader.getKeypleCalypsoSam().getLegacySam();

        log.info("Pre personalization card {} with {}, from {} to {}",
                HexUtil.toHex(calypsoCard.getApplicationSerialNumber()),
                keyGenerate,
                startDate,
                endDate);

        switch (keyGenerate) {
            case CARD:
                cardSamKeyPair(reader, startDate, endDate);
            break;
            case LEGACY_SAM:
                legacySamKeyPair(reader, startDate, endDate);
                break;
        }

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .build();
    }

    private void cardSamKeyPair(KeypleReader reader, LocalDate startDate, LocalDate endDate) {
        CalypsoCard calypsoCard = reader.getCalypsoCard();
        LegacySam legacySam = reader.getKeypleCalypsoSam().getLegacySam();

        try {
            reader.getFreeTransactionManager()
                    .prepareGenerateAsymmetricKeyPair()
                    .prepareGetData(org.eclipse.keypop.calypso.card.GetDataTag.CARD_PUBLIC_KEY)
                    .processCommands(ChannelControl.KEEP_OPEN);
        } catch (Exception e) {
            throw new CardException("Error making ECC key pair: " + e.getMessage());
        }

        LegacyCardCertificateComputationData cardCertificateComputationData =
                KeypleCalypsoSam.legacySamExtensionService.getLegacySamApiFactory()
                        .createLegacyCardCertificateComputationData()
                        .setCardAid(calypsoCard.getDfName())
                        .setCardSerialNumber(calypsoCard.getApplicationSerialNumber())
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .setCardStartupInfo(calypsoCard.getStartupInfoRawData());

        try {
            reader.getKeypleCalypsoSam().getFreeTransactionManager()
                    .prepareGetData(GetDataTag.CA_CERTIFICATE)
                    .prepareComputeCardCertificate(cardCertificateComputationData)
                    .processCommands();
        } catch (Exception e) {
            throw new SamException("Error generating PKI sam certification: " + e.getMessage());
        }

        try {
            reader.getFreeTransactionManager()
                    .preparePutData(PutDataTag.CA_CERTIFICATE, legacySam.getCaCertificate())
                    .preparePutData(PutDataTag.CARD_CERTIFICATE, cardCertificateComputationData.getCertificate())
                    .processCommands(ChannelControl.CLOSE_AFTER);
        } catch (Exception e) {
            throw new CardException("Error putting sam certification data on card: " + e.getMessage());
        }

    }

    private void legacySamKeyPair(KeypleReader reader, LocalDate startDate, LocalDate endDate) {
        KeyPairContainer keyPairContainer = KeypleCalypsoSam.legacySamExtensionService.getLegacySamApiFactory().createKeyPairContainer();
        CalypsoCard calypsoCard = reader.getCalypsoCard();
        LegacySam legacySam = reader.getKeypleCalypsoSam().getLegacySam();

        LegacyCardCertificateComputationData cardCertificateComputationData =
                KeypleCalypsoSam.legacySamExtensionService.getLegacySamApiFactory()
                        .createLegacyCardCertificateComputationData()
                        .setCardAid(calypsoCard.getDfName())
                        .setCardSerialNumber(calypsoCard.getApplicationSerialNumber())
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .setCardStartupInfo(calypsoCard.getStartupInfoRawData());

        try {
            reader.getKeypleCalypsoSam().getFreeTransactionManager()
                    .prepareGetData(GetDataTag.CA_CERTIFICATE)
                    .prepareGenerateCardAsymmetricKeyPair(keyPairContainer)
                    .prepareComputeCardCertificate(cardCertificateComputationData)
                    .processCommands();
        } catch (Exception e) {
            throw new SamException("Error generating PKI sam certification: " + e.getMessage());
        }

        try {
            reader.getFreeTransactionManager()
                    .preparePutData(PutDataTag.CA_CERTIFICATE, legacySam.getCaCertificate())
                    .preparePutData(PutDataTag.CARD_KEY_PAIR, keyPairContainer.getKeyPair())
                    .preparePutData(PutDataTag.CA_CERTIFICATE, cardCertificateComputationData.getCertificate())
                    .processCommands(ChannelControl.KEEP_OPEN);
        }catch (Exception e) {
            throw new CardException("Error putting sam certification data on card: " + e.getMessage());
        }
    }
}
