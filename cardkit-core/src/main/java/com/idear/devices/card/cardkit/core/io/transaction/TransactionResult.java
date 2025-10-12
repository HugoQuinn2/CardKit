package com.idear.devices.card.cardkit.core.io.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Getter
@Setter
public class TransactionResult<T> extends Item {
    private String transactionName;
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

    public TransactionResult<T> print() {
        System.out.println(this.toJson());
        return this;
    }

    @Override
    public String toString() {
        return String.format(
                "[%s - %s] > %s {%s ms}",
                transactionName.toUpperCase(),
                transactionStatus,
                message,
                time
        );
    }

}
