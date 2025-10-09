package com.idear.devices.card.cardkit.core.utils;

import java.io.ByteArrayOutputStream;

public class ByteBuilder {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public void append(byte b) {
        buffer.write(b);
    }

    public void append(byte[] bytes) {
        buffer.write(bytes, 0, bytes.length);
    }

    public void append(int value) {
        buffer.write(value & 0xFF);
    }

    public byte[] toByteArray() {
        return buffer.toByteArray();
    }

    public int size() {
        return buffer.size();
    }

    public void clear() {
        buffer.reset();
    }

}
