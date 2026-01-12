package com.idear.devices.card.cardkit.core.utils;

public class Strings {
    public static String extractBetween(String text, String start, String end) {
        if (text == null || start == null || end == null) {
            return null;
        }

        String data = text.toUpperCase();
        String s = start.toUpperCase();
        String e = end.toUpperCase();

        int startIndex = data.indexOf(s);
        if (startIndex == -1) return null;

        startIndex += s.length();

        int endIndex = data.indexOf(e, startIndex);
        if (endIndex == -1) return null;

        return data.substring(startIndex, endIndex);
    }

    public static String normalizeClassName(String className) {
        if (className == null || className.isEmpty()) return className;

        String result = className
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");

        return result.toUpperCase();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static String intToHexByte(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Value out of byte range");
        }
        return String.format("%02X", value);
    }
}
