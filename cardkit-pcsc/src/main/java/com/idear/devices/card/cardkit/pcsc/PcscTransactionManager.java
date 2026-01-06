package com.idear.devices.card.cardkit.pcsc;

public class PcscTransactionManager {
    private PcscAbstractReader cardReader;
    private PcscAbstractReader samReader;

    public PcscTransactionManager prepareTransactionManager(PcscAbstractReader cardReader, PcscAbstractReader samReader) throws Exception {
        this.cardReader = cardReader;
        this.samReader = samReader;
        return this;
    }

    public PcscTransactionManager openSession(byte key) {
        return this;
    }
}
