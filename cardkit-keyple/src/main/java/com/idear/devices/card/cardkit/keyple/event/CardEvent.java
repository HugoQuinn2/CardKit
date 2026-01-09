package com.idear.devices.card.cardkit.keyple.event;

import com.idear.devices.card.cardkit.core.io.reader.AbstractReader;
import lombok.Data;

@Data
public class CardEvent {
    private final AbstractReader reader;
    private final CardStatus cardStatus;
}
