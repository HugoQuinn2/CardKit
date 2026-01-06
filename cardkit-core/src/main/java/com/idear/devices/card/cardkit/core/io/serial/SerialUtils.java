package com.idear.devices.card.cardkit.core.io.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
public abstract class SerialUtils {

    /**
     * Search all OS ports and verify if the port exist.
     * @param name the name port
     * @return the {@link SerialPort} founded.
     * @throws IllegalArgumentException if {@code name} port not founded
     */
    public static SerialPort safeGetPort(String name) {
        try {
            Class.forName("com.fazecast.jSerialComm.SerialPort");

            SerialPort.getCommPorts();
            SerialPort port = SerialPort.getCommPort(name);

            if (port == null)
                throw new IllegalArgumentException("No port found: " + name);

            return port;
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Error while getting port: " + name + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        }
    }

    public static SerialPort[] getCommPorts() {
        try {
            Class.forName("com.fazecast.jSerialComm.SerialPort");

            return SerialPort.getCommPorts();
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Error while getting ports: " + t.getMessage(), t);
        }
    }

    /**
     * Parse string {@code ASCII} to frame. With {@link StandardCharsets} UTF_8.
     * @param data the string.
     * @return the parsed frame
     */
    public static byte[] stringToBytes(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Remove echo from response if this exists.
     * @param response the response to remove echo.
     * @param request the original request.
     * @return response without echo.
     */
    public static byte[] removeEcho(byte[] response, byte[] request) {
        if (response.length >= request.length) {
            boolean echoMatches = true;
            for (int i = 0; i < request.length; i++) {
                if (response[i] != request[i]) {
                    echoMatches = false;
                    break;
                }
            }

            if (echoMatches) {
                return Arrays.copyOfRange(response, request.length, response.length);
            }
        }
        return response;
    }

}
