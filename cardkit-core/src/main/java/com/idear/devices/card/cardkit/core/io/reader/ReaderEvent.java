package com.idear.devices.card.cardkit.core.io.reader;

public interface ReaderEvent<T> {
    void onEvent(T data);
}
