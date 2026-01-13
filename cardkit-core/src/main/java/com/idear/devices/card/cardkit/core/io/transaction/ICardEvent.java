package com.idear.devices.card.cardkit.core.io.transaction;

public interface ICardEvent {
    void onEvent(CardStatus cardStatus);
}
