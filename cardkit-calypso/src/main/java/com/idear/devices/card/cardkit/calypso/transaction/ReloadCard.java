package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;

import java.util.function.Predicate;

public class ReloadCard extends Transaction<CalypsoCardCDMX, ReaderPCSC> {

    private final Predicate<CalypsoCardCDMX> valid;
    private final int amount;

    public ReloadCard(Predicate<CalypsoCardCDMX> valid, int amount) {
        super("reload");
        this.valid = valid;
        this.amount = amount;
    }

    @Override
    public TransactionResult<CalypsoCardCDMX> execute(ReaderPCSC reader) {
        return null;
    }
}
