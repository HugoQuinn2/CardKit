package com.idear.devices.card.cardkit.core.io.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
public class TransactionResult<T> extends Item {
    private TransactionStatus transactionStatus;
    private T data;
    private String message;
    private long time;

    @JsonIgnore
    public boolean is(TransactionStatus transactionStatus) {
        return this.transactionStatus.equals(transactionStatus);
    }

    @JsonIgnore
    public boolean isOk() {
        return is(TransactionStatus.OK);
    }

}
