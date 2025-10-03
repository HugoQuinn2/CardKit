package com.idear.devices.card.cardkit.core.io.card;

public interface IParseCard<T extends Card<T>>{
    T parse(Object cardData);
    String getParseKey();
}
