package com.idear.devices.card.cardkit.core.io.transaction.data;

import java.time.LocalDateTime;

public interface ITransactionDataEvent {
    LocalDateTime timestamp();
    TransactionOperationType operationType();
}
