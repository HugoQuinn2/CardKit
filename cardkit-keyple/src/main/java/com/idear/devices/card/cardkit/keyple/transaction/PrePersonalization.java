package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;


import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
public class PrePersonalization extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    public enum KeyGenerated {
        CARD,
        LEGACY_SAM
    }

    private final KeyGenerated keyGenerate;
    private final LocalDate startDate;
    private final LocalDate endDate;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {
        CalypsoCard calypsoCard = context.getKeypleCardReader().getCalypsoCard();
        CardReader cardReader = context.getKeypleCardReader().getCardReader();
        CardReader samReader = context.getKeypleCalypsoSamReader().getSamReader();

        log.info("Pre personalization card {} with {}, from {} to {}",
                HexUtil.toHex(calypsoCard.getApplicationSerialNumber()),
                keyGenerate, startDate, endDate
        );

        LegacySam legacySam = KeypleUtil.selectLegacySamByProduct(
                context.getKeypleCalypsoSamReader().getSamReader(),
                LegacySam.ProductType.SAM_C1
        );

        switch (keyGenerate) {
            case CARD:
                KeypleUtil.cardSamKeyPair(
                        cardReader,
                        samReader,
                        legacySam,
                        calypsoCard,
                        startDate,
                        endDate
                );
            break;
            case LEGACY_SAM:
                KeypleUtil.legacySamKeyPair(
                        cardReader,
                        samReader,
                        legacySam,
                        calypsoCard,
                        startDate,
                        endDate
                );
                break;
        }

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .build();
    }

}
