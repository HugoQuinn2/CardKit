package com.idear.devices.card.cardkit.core.utils;

public abstract class ExceptionUtils {

    public static RuntimeException wrap(RuntimeException e, String message, Object... o) {
        String msg = String.format(message, o);

        try {
            return e.getClass()
                    .getConstructor(String.class)
                    .newInstance(msg);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
