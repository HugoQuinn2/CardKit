package com.idear.devices.card.cardkit.core.io.transaction.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class AbstractTransactionDataEvent implements ITransactionDataEvent {

    private final LocalDateTime timestamp = LocalDateTime.now();

    @Override
    public LocalDateTime timestamp() {
        return timestamp;
    }

}
