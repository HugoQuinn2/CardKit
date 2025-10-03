package com.idear.devices.card.cardkit.core.utils;

public abstract class Assert {

    public static <T> T isNull(T object, String message, Object... o) {
        if (object == null) {
            throw new NullPointerException(String.format(message, o));
        }
        return object;
    }

    public static <T> T isNullAndUpdate(T field, T data, String message, Object... o) {
        if (data == null)
            throw new NullPointerException(String.format(message, o));

        if (field == null)
            return data;
        else
            return field;
    }

}
