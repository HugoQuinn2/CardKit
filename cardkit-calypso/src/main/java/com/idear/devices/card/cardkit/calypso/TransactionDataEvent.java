package com.idear.devices.card.cardkit.calypso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.file.DebitLog;
import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.calypso.file.LoadLog;
import com.idear.devices.card.cardkit.core.datamodel.calypso.Profile;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
public class TransactionDataEvent extends Item {

    private final Event event;
    private final LocationCode locationCode;
    private final Profile profile;
    private final int balanceBeforeTransaction;
    private final int transactionAmount;
    private final String samSerial;
    private final DebitLog debitLog;
    private final LoadLog loadLog;
    private final Contract contract;
    private final String mac;

    @Builder.Default
    @JsonIgnore
    private final LocalDateTime transactionDateTime = LocalDateTime.now();
}
