package com.idear.devices.card.cardkit.keyple.transaction;

import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file.Event;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file.Logs;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionContext;
import com.idear.devices.card.cardkit.keyple.KeypleUtil;
import com.idear.devices.card.cardkit.keyple.TransactionDataEvent;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

@RequiredArgsConstructor
public class RehabilitateCard extends AbstractTransaction<Boolean, KeypleTransactionContext> {

    private final CalypsoCardCDMX calypsoCardCDMX;

    @Override
    public TransactionResult<Boolean> execute(KeypleTransactionContext context) {

        if (calypsoCardCDMX.isEnabled())
            throw new CardException("card already rehabilitate");

        context.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION)
                .prepareRehabilitate()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.KEEP_OPEN);

        return TransactionResult
                .<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .build();
    }

}
