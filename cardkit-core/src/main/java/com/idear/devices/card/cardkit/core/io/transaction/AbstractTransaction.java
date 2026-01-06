package com.idear.devices.card.cardkit.core.io.transaction;

import lombok.Data;

@Data
public abstract class AbstractTransaction<T, R extends AbstractTransactionContext> {
    public abstract TransactionResult<T> execute(R context);
}
