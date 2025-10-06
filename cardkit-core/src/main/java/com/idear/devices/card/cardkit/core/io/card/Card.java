package com.idear.devices.card.cardkit.core.io.card;

import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class Card extends Item {
    private String serial;
    private int balance;

    public Card(String serial) {
        this.serial = serial;
    }

    public Card() {
        this("");
    }

}
