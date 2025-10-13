package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a compact time format where the time of day is stored as
 * the number of minutes since midnight (00:00).
 * <p>
 * The reference time zone used by this class is based on the system's
 * default time zone, following the ISO 8601 standard for date and time
 * representation. ISO 8601 ensures consistent formatting and parsing
 * of date-time values across systems.
 * </p>
 *
 * <p>
 * For example:
 * <ul>
 *   <li>{@code CompactTime.now()} returns the current time in minutes from midnight.</li>
 *   <li>{@code CompactTime.fromLocalDateTime(LocalDateTime)} converts a LocalDateTime
 *       to a CompactTime using the local system offset.</li>
 * </ul>
 * </p>
 *
 * @see java.time.LocalTime
 * @see java.time.LocalDateTime
 * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 Standard</a>
 */
@Getter
public class CompactTime extends Item implements IDataModel {

    /**
     * The reference offset time (00:00), representing midnight.
     * All other times are measured as minutes from this point.
     */
    public static final LocalTime OFFSET = LocalTime.MIDNIGHT;

    /** The LocalTime representation of this compact time. */
    private final LocalTime time;

    /** The number of minutes since {@link #OFFSET}. */
    private final int value;

    /**
     * Constructs a CompactTime from the specified minute value.
     *
     * @param value number of minutes since midnight (00:00)
     */
    public CompactTime(int value) {
        this.value = value;
        this.time = OFFSET.plusMinutes(value);
    }

    /**
     * Returns a CompactTime representing the current system time,
     * using the system default time zone (as per ISO 8601).
     *
     * @return CompactTime instance representing the current time
     */
    public static CompactTime now() {
        return fromLocalDateTime(LocalDateTime.now());
    }

    /**
     * Converts this CompactTime to a two-byte representation.
     * The first byte is the most significant byte (MSB),
     * and the second is the least significant byte (LSB).
     *
     * @return byte array of size 2 representing this time
     */
    public byte[] toBytes() {
        byte msb = (byte) ((value & 0xFF00) >> 8);
        byte lsb = (byte) (value & 0x00FF);
        return new byte[]{msb, lsb};
    }

    /**
     * Creates a CompactTime from the given number of minutes.
     *
     * @param minutes minutes since midnight (00:00)
     * @return CompactTime instance
     */
    public static CompactTime fromMinutes(int minutes) {
        return new CompactTime(minutes);
    }

    /**
     * Creates a CompactTime from a LocalDateTime, calculating
     * the number of minutes since midnight based on the local time.
     *
     * @param localDateTime the LocalDateTime to convert
     * @return CompactTime instance
     */
    public static CompactTime fromLocalDateTime(LocalDateTime localDateTime) {
        int minutes = (int) ChronoUnit.MINUTES.between(OFFSET, localDateTime.toLocalTime());
        return new CompactTime(minutes);
    }

    /**
     * Creates a CompactTime from a LocalTime, calculating
     * the number of minutes since midnight based on the local time.
     *
     * @param localTime the LocalTime to convert
     * @return CompactTime instance
     */
    public static CompactTime fromLocalTime(LocalTime localTime) {
        int minutes = (int) ChronoUnit.MINUTES.between(OFFSET, localTime);
        return new CompactTime(minutes);
    }

    /**
     * Checks whether this CompactTime represents the OFFSET time (empty state).
     *
     * @return {@code true} if the time equals the OFFSET, {@code false} otherwise
     */
    public boolean isEmpty() {
        return time.equals(OFFSET);
    }

    /**
     * Returns the JSON representation of this time.
     *
     * @return a string formatted according to ISO 8601 local time
     */
    @JsonValue
    public String toJsonValue() {
        return time.toString();
    }
}
