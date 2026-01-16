package com.idear.devices.card.cardkit.core.io.apdu;

import com.idear.devices.card.cardkit.core.exception.ApduResponseException;

import javax.smartcardio.ResponseAPDU;
import java.io.Serializable;
import java.util.function.Predicate;

public class ResponseApdu implements Serializable {
    private ResponseAPDU apdu;

    public ResponseApdu(byte[] bytes) {
        this.apdu = new ResponseAPDU(bytes);
    }

    public int getSW() {
        return apdu.getSW();
    }

    public int getSW1() {
        return apdu.getSW1();
    }

    public int getSW2() {
        return apdu.getSW2();
    }

    public byte[] getBytes() {
        return apdu.getBytes();
    }

    public byte[] getData() {
        return apdu.getData();
    }

    public boolean isSuccess() {
        return getSW() == 0x9000;
    }

    public ResponseApdu throwIsNotSuccess() {
        if (!isSuccess())
            throw new ApduResponseException("invalid status response", getSW());

        return this;
    }

    public ResponseApdu throwIsCondition(Predicate<Integer> predicate) {
        int sw = getSW();
        if (predicate.test(sw))
            throw new ApduResponseException("invalid status condition", sw);

        return this;
    }

}
