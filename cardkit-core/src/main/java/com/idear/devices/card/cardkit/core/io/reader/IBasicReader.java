package com.idear.devices.card.cardkit.core.io.reader;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public interface IBasicReader {
    ResponseAPDU simpleCommand(CommandAPDU command);
}
