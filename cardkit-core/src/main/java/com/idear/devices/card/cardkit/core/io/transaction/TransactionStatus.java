package com.idear.devices.card.cardkit.core.io.transaction;

import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.exception.SamException;
import com.idear.devices.card.cardkit.core.io.reader.Reader;

public enum TransactionStatus {
    /** Status returned when the transaction was successful */
    OK,
    /** Status returned when a card kit exception [{@link CardException}, {@link SamException} and {@link ReaderException}]
     * is throwing on {@link Transaction#execute(Reader)}*/
    ABORTED,
    /** Status returned when a when an {@link Exception} error occurs on {@link Transaction#execute(Reader)}*/
    ERROR
}
