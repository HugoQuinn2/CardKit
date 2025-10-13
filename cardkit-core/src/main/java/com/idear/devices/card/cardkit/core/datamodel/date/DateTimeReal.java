package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a real date-time value as the number of seconds elapsed
 * since a fixed reference date-time (OFFSET).
 * <p>
 * The reference date-time is {@code 1997-01-01T00:00:00}, following the ISO 8601 standard.
 * </p>
 *
 * <p>
 * Example usage:
 * <ul>
 *     <li>{@code DateTimeReal.now()} returns the current system date-time.</li>
 *     <li>{@code DateTimeReal.fromLocalDateTime(LocalDateTime)} creates a DateTimeReal from a LocalDateTime.</li>
 *     <li>{@code toBytes()} returns a 2-byte representation of the seconds value (MSB first).</li>
 * </ul>
 * </p>
 *
 * @see java.time.LocalDateTime
 * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 Standard</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DateTimeReal extends Item implements IDataModel {

    /**
     * Reference date-time (OFFSET) used as the base for calculating seconds.
     * All date-time values are measured as seconds since this point.
     */
    public static final LocalDateTime OFFSET = LocalDateTime.of(1997, 1, 1, 0, 0, 0);

    /** The LocalDateTime represented by this instance. */
    private final LocalDateTime dateTime;

    /** The number of seconds elapsed since {@link #OFFSET}. */
    private final int value;

    /**
     * Returns a DateTimeReal representing the current system date-time.
     *
     * @return DateTimeReal instance for now
     */
    public static DateTimeReal now() {
        return fromLocalDateTime(LocalDateTime.now());
    }

    /**
     * Constructs a DateTimeReal from a given number of seconds since OFFSET.
     *
     * @param value seconds since OFFSET
     */
    private DateTimeReal(int value) {
        this.value = value;
        this.dateTime = OFFSET.plusSeconds(value);
    }

    /**
     * Creates a DateTimeReal from a number of seconds since OFFSET.
     *
     * @param seconds seconds since OFFSET
     * @return DateTimeReal instance
     */
    public static DateTimeReal fromSeconds(int seconds) {
        return new DateTimeReal(seconds);
    }

    /**
     * Creates a DateTimeReal from a LocalDateTime.
     *
     * @param localDateTime the LocalDateTime to convert
     * @return DateTimeReal instance
     */
    public static DateTimeReal fromLocalDateTime(LocalDateTime localDateTime) {
        int seconds = (int) ChronoUnit.SECONDS.between(OFFSET, localDateTime);
        return fromSeconds(seconds);
    }

    /**
     * Converts this DateTimeReal to a 2-byte representation.
     * The first byte is the most significant byte (MSB),
     * and the second byte is the least significant byte (LSB).
     *
     * <p>
     * Note: Only the lower 16 bits of the seconds value are stored.
     * </p>
     *
     * @return byte array of size 2 representing this value
     */
    public byte[] toBytes() {
        byte msb = (byte) ((value & 0xFF00) >> 8);
        byte lsb = (byte) (value & 0x00FF);
        return new byte[]{msb, lsb};
    }

    /**
     * Returns the ISO 8601 string representation of this date-time
     * for JSON serialization.
     *
     * @return ISO 8601 formatted date-time string
     */
    @JsonValue
    public String toJsonValue() {
        return dateTime.toString();
    }
}
