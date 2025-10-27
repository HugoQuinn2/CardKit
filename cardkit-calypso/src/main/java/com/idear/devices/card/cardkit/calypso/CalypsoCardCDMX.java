package com.idear.devices.card.cardkit.calypso;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Product;
import com.idear.devices.card.cardkit.core.io.card.Card;
import com.idear.devices.card.cardkit.calypso.file.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CalypsoCardCDMX extends Card {
    private Product product;
    private Environment environment;
    private Events events = new Events();
    private Contracts contracts = new Contracts();
    private DebitLog debitLog;
    private LoadLog loadLog;

    private boolean enabled;
}
