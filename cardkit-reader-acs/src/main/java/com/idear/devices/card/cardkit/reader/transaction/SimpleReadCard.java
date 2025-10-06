package com.idear.devices.card.cardkit.reader.transaction;

import com.idear.devices.card.cardkit.core.io.datamodel.calypso.AppSubType;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.reader.CalypsoCDMXCard;
import com.idear.devices.card.cardkit.reader.ReaderPCSC;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SimpleReadCard extends Transaction<CalypsoCDMXCard, ReaderPCSC> {

    public SimpleReadCard() {
        super("simple read card");
    }

    @Override
    public TransactionResult<CalypsoCDMXCard> execute(ReaderPCSC reader) {
        CalypsoCDMXCard calypsoCDMXCard = new CalypsoCDMXCard();

        reader.updateCalypsoCardSession();
        CalypsoCard calypsoCard = reader.getCalypsoCard();

        calypsoCDMXCard.setSerial(
                HexUtil.toHex(calypsoCard.getApplicationSerialNumber())
        );

        // File structure
        int appSubType = calypsoCard.getApplicationSubtype() & 0xff;
        if (appSubType != AppSubType.CDMX_RT.getValue())
            throw new CardException("Unexpected file structure");

        // Invalid DF
        if (calypsoCard.isDfInvalidated())
            throw new CardException("invalid df status");

        return TransactionResult
                .<CalypsoCDMXCard>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(calypsoCDMXCard)
                .build();
    }
}
