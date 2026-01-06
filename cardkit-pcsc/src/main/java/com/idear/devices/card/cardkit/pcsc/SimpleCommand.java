package com.idear.devices.card.cardkit.pcsc;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SimpleCommand extends AbstractTransaction<ResponseAPDU, PcscAbstractReader> {

    private final CommandAPDU commandAPDU;

    public SimpleCommand(CommandAPDU commandAPDU) {
        super("SIMPLE_COMMAND");
        this.commandAPDU = commandAPDU;
    }

    @Override
    public TransactionResult<ResponseAPDU> execute(PcscAbstractReader reader) {
        reader.connectToCard();
        ResponseAPDU responseAPDU;
        try {
            responseAPDU = reader.getCardChannel().transmit(commandAPDU);
        } catch (CardException e) {
            throw new com.idear.devices.card.cardkit.core.exception.CardException(e.getMessage());
        }
        return TransactionResult.<ResponseAPDU>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(responseAPDU)
                .build();
    }

}
