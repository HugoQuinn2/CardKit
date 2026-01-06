package com.idear.devices.card.cardkit.pcsc;

import com.idear.devices.card.cardkit.core.utils.ByteUtils;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;
import java.util.List;

public abstract class PcscUtil {

    public static List<CardTerminal> getCardTerminals() throws CardException {
        return TerminalFactory.getDefault().terminals().list();
    }

    public static CardTerminal getCardTerminalMatchingName(String namePattern) throws CardException {
        List<CardTerminal> cardTerminals = TerminalFactory.getDefault().terminals().list();
        if (cardTerminals.isEmpty())
            throw new RuntimeException("no readers available");

        for (CardTerminal cardTerminal : cardTerminals)
            if (cardTerminal.getName().matches(namePattern))
                return cardTerminal;

        throw new RuntimeException(cardTerminals.size() + " card readers found, none matched the pattern");
    }

    public static CommandAPDU buildCommand(byte... bytes) {
        return new CommandAPDU(bytes);
    }

    public static CommandAPDU buildCommand(String header, byte... data) {
        byte[] headerBytes = ByteUtils.hexToBytes(header);

        byte[] apdu = new byte[5 + data.length + 1];
        apdu[0] = headerBytes[0];   // CLA
        apdu[1] = headerBytes[1];   // INS
        apdu[2] = headerBytes[2];   // P1
        apdu[3] = headerBytes[3];  // P2
        apdu[4] = (byte) data.length; // Lc

        System.arraycopy(data, 0, apdu, 5, data.length);

        apdu[5 + data.length] = 0x00;

        return new CommandAPDU(apdu);
    }

}
