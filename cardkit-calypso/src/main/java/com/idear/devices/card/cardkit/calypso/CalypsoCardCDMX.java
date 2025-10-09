package com.idear.devices.card.cardkit.calypso;

import com.idear.devices.card.cardkit.core.io.card.Card;
import com.idear.devices.card.cardkit.calypso.file.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class CalypsoCardCDMX extends Card {
    private Environment environment;
    private Events events = new Events();
    private Contracts contracts = new Contracts();
    private DebitLog debitLog;
    private LoadLog loadLog;
}
