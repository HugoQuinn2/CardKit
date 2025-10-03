package com.idear.devices.card.cardkit.core.io.transaction;

import com.idear.devices.card.cardkit.core.io.reader.Reader;
import lombok.Data;

@Data
public abstract class Transaction<T, R extends Reader<?>> {
    private final String name;
    public abstract TransactionResult<T> execute(R reader);
}
