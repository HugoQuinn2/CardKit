package com.idear.devices.card.cardkit.reader;

import com.idear.devices.card.cardkit.reader.file.*;
import lombok.Data;

import java.util.*;

@Data
public class CalypsoCDMXCard {

    private String serial = "";
    private int balance = 0;

    private Environment environment;
    private List<Event> events = new ArrayList<>();
    private Contracts contracts = new Contracts();
    private DebitLog debitLog;
    private LoadLog loadLog;
}
