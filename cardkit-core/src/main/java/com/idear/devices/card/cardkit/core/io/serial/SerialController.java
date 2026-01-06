package com.idear.devices.card.cardkit.core.io.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * <h4>Serial connection controller with automatic reconnection support.</h4>
 *
 * <p>
 * This class implements a Singleton pattern to ensure there is only
 * one active instance of the serial connection. It provides functionality for:
 * </p>
 *
 * <ul>
 *   <li>Connecting and disconnecting a serial port.</li>
 *   <li>Sending commands and waiting for responses with configurable timeouts.</li>
 *   <li>Listening for incoming data in a separate thread.</li>
 *   <li>Automatically retrying the connection in case of disconnection.</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 *     SerialConfig serialConfig = SerialConfig.builder().portName("COM8").build();
 *     SerialController controller = SerialController.getInstance(serialConfig);
 *     byte[] response = controller.sendCommand(1000, new byte[]{0x01, 0x02});
 * </pre>
 *
 * <p>Thread-safe and supports automatic reconnection in case of port loss.</p>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0
 * @since 1.0
 */

@Data
@Slf4j
public class SerialController {
    private static final Map<String, SerialController> instances = new ConcurrentHashMap<>();

    private SerialPort serialPort;
    private final SerialConfig serialConfig;
    private final BlockingQueue<byte[]> responseQueue = new LinkedBlockingQueue<>();
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private boolean reconnecting = false;
    private boolean autoReconnecting = true;
    private Thread reconnectingThread;
    private final long reconnectingTimeOut = 2000;

    /**
     * Create a serial controller with custom configuration and start port listener.
     */
    private SerialController(SerialConfig config) {
        long time = System.currentTimeMillis();
        this.serialConfig = config;
        connect();
        log.debug("New serial controller instance created {}@{} {} ms", serialConfig.getPortName(),
                serialConfig.getBaudRate(), (System.currentTimeMillis() - time));
    }

    /**
     * Establish the connection to the {@link SerialPort} using the parameters from {@link SerialConfig},
     * stop the recovery thread if the connection is successful.
     * @return if the connection was correct. Existing {@link SerialPort} and that is not in use.
     */
    public boolean connect() {
        try {
            this.serialPort = SerialUtils.safeGetPort(serialConfig.getPortName());
            serialPort.setComPortParameters(serialConfig.getBaudRate(), serialConfig.getDataBits(),
                    serialConfig.getStopBits(), serialConfig.getParity());
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
            openPort();
            startListener();

            if (reconnecting && isConnected()) {
                if (reconnectingThread != null && reconnectingThread.isAlive()) {
                    reconnectingThread.interrupt();
                    reconnectingThread = null;
                }
                reconnecting = false;
            }


            return true;
        } catch (IllegalArgumentException e) {
            if (!reconnecting) {
                throw new SerialPortConnectionLostException(e.getMessage());
            }
            return false;
        } catch (Exception e) {
            if (!reconnecting) {
                log.error("Error creating serial controller {}@{}, e: {}", serialConfig.getPortName(),
                        serialConfig.getBaudRate(), e.getMessage());
                throw new SerialPortConnectionLostException(e.getMessage());
            }
            return false;
        }
    }

    /**
     * Closes the {@link SerialPort} connection and removes all listeners, the recovery thread
     * {@code reconnectingThread} does not stop.
     */
    public void disconnect() {
        if (isConnected()) {
            serialPort.removeDataListener();
            serialPort.closePort();
            serialPort = null;

            log.info("Disconnected from serial port: {}", serialConfig.getPortName());
        }

        instances.remove(serialConfig.getPortName());
    }

    /**
     * Create or get an instance for {@link SerialController} based on the name of the port {@link SerialPort},
     * a new instance is generated if the port is different from the registered ones {@code instances} and establish
     * the connection.
     * @param config the serial config to use.
     * @return the instance for {@link SerialController}
     */
    public static synchronized SerialController getInstance(SerialConfig config) {
        return instances.computeIfAbsent(config.getPortName(), key -> new SerialController(config));
    }

    /**
     * Open the port from {@link SerialPort}, necessary to establish connection.
     */
    private void openPort() {
        log.debug("Opening port {} @{}.", serialPort.getSystemPortName(), serialPort.getBaudRate());

        if (!serialPort.isOpen()) {
            if (!serialPort.openPort()) {
                if (!reconnecting)
                    log.error("Could not open port: {} @{}, Error: {}.", serialPort.getSystemPortName(), serialPort.getBaudRate(), serialPort.getLastErrorCode());
                throw new SerialPortConnectionLostException(String.format("Could not open port: %s @%s, Error: %s.",
                        serialPort.getSystemPortName(), serialPort.getBaudRate(), serialPort.getLastErrorCode()));
            }
        }
    }

    /**
     * Start the data listener to capture incoming data {@code  SerialPort.LISTENING_EVENT_DATA_AVAILABLE} and port
     * disconnected {@code SerialPort.LISTENING_EVENT_PORT_DISCONNECTED}
     */
    private void startListener() {
        serialPort.removeDataListener();
        serialPort.addDataListener(new SerialPortDataListener() {

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE |
                        SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                switch (event.getEventType()) {
                    case SerialPort.LISTENING_EVENT_DATA_AVAILABLE:
                        onDataAvailable();
                        break;
                    case SerialPort.LISTENING_EVENT_PORT_DISCONNECTED:
                        onDisconnect();
                        break;
                }
            }
        });
    }

    /**
     * Read the data available, write on buffer and offer on response queue, just called on
     * {@code  SerialPort.LISTENING_EVENT_DATA_AVAILABLE}.
     */
    private void onDataAvailable() {
        try {
            while (true) {
                if (!isConnected()) {
                    responseQueue.clear();
                    buffer.reset();
                    break;
                }

                int available = serialPort.bytesAvailable();
                if (available <= 0) break;

                byte[] readBuffer = new byte[available];
                int bytesRead = serialPort.readBytes(readBuffer, readBuffer.length);

                if (bytesRead > 0) {
                    buffer.write(readBuffer, 0, bytesRead);
                }

                safeWait(serialConfig.getReadIntervalDelay());
            }

            responseQueue.offer(buffer.toByteArray());
            log.trace("CcTalk bytes received on [{}]: {}",
                    serialPort.getSystemPortName(),
                    Arrays.toString(buffer.toByteArray()));

            buffer.reset();
        } catch (Exception e) {
            log.error("Error reading data from port {}: {}", serialPort.getSystemPortName(), e.getMessage());
        }
    }

    /**
     * Start a reconnecting thread if the {@link SerialPort} it gets lost, just called on
     * {@code SerialPort.LISTENING_EVENT_PORT_DISCONNECTED}
     */
    private void onDisconnect() {
        if (reconnecting && autoReconnecting)
            return;

        reconnecting = true;
        reconnectingThread = new Thread(() -> {
            log.info("Attempting automatic reconnection to {}", serialPort.getSystemPortName());
            disconnect();

            while (reconnecting) {
                try {
                    boolean success = connect();
                    if (success) {
                        log.info("Reconnected successfully to {}", serialConfig.getPortName());
                        break;
                    }
                } catch (Exception e) {
                    // just nothing
                } finally {
                    safeWait(reconnectingTimeOut);
                }
            }

        }, "serial-reconnect-thread-" + serialConfig.getPortName());

        reconnectingThread.setDaemon(true);
        reconnectingThread.start();
    }

    /**
     * Write bytes frame on {@link SerialPort}, only if the connection is established.
     * @param data the frame to write.
     */
    public synchronized void write(byte... data) {
        if (!reconnecting || isConnected()) {
            serialPort.writeBytes(data, data.length);
        }
    }

    /**
     * Send a byte frame and wait {@code 1000 ms} for the response.
     * @param frame the bytes frame to send.
     * @return the bytes frame response.
     * @throws SerialResponseException if the response is not obtained within {@code 1000 ms} and if the {@link SerialPort}
     * is not connected.
     */
    public byte[] sendCommand(byte... frame) throws SerialResponseException {
        return sendCommand(1000, frame);
    }

    /**
     * Send a byte frame and wait custom time on {@code ms} for the response.
     * @param timeoutMillis the time to wait.
     * @param request the bytes frame to send.
     * @return the bytes frame response.
     * @throws SerialResponseException if the response is not obtained within {@code 1000 ms} and if the {@link SerialPort}
     * is not connected.
     */
    public byte[] sendCommand(long timeoutMillis, byte... request) throws SerialResponseException {
        synchronized (this) {
            if (reconnecting || !isConnected())
                throw new SerialResponseException("Connection lost, commands cannot be sent while the connection is being restored.");

            responseQueue.clear();
            buffer.reset();

            write(request);

            long deadline = System.currentTimeMillis() + timeoutMillis;

            try {
                while (true) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        throw new SerialResponseException(String.format(
                                "No valid response from CcTalk device [%s] in %sms for command: %s",
                                request[0], timeoutMillis, Arrays.toString(request)
                        ));
                    }

                    byte[] response = responseQueue.poll(remaining, TimeUnit.MILLISECONDS);

                    if (response == null) {
                        throw new SerialResponseException(String.format(
                                "No response from CcTalk device [%s] in %sms for command: %s",
                                request[0], timeoutMillis, Arrays.toString(request)
                        ));
                    }

                    if (Arrays.equals(response, request)) {
                        log.trace("Ignoring echo: {}", Arrays.toString(response));
                        continue;
                    }

                    log.trace("Response from device [{}] for command {} -> {}",
                            request[0], Arrays.toString(request), Arrays.toString(response));
                    return SerialUtils.removeEcho(response, request);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SerialResponseException("Interrupted while waiting for response");
            }
        }
    }

    /**
     * If the {@link SerialPort} is not null and is open.
     * @return is not null and is open.
     */
    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen();
    }

    private static void safeWait(long time) {
        try {
            if (time < 0) {
                while (true) {
                    Thread.sleep(10);
                }
            } else {
                Thread.sleep(time);
            }
        } catch (InterruptedException e) {
            // just nothing
        }
    }

}
