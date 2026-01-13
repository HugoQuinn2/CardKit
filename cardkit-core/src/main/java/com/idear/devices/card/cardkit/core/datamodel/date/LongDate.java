package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Represents a date encoded in a numeric hexadecimal format: {@code AAAAMMDDh},
 * where:
 * <ul>
 *     <li>AAAA = year (4 digits)</li>
 *     <li>MM = month (2 digits)</li>
 *     <li>DD = day (2 digits)</li>
 * </ul>
 *
 * <p>
 * A value of 0 represents an empty or missing date.
 * </p>
 *
 * <p>
 * Example usage:
 * <ul>
 *     <li>{@code LongDate.fromLocalDate(LocalDate.of(2025, 10, 13))}</li>
 *     <li>{@code LongDate.fromValue(20251013)}</li>
 *     <li>{@code LongDate.fromValue(0)} // represents empty date</li>
 * </ul>
 * </p>
 */
@Getter
public class LongDate extends Item implements IDataModel {

    /** Numeric value of the date in AAAAMMDD format. 0 represents an empty date. */
    private final int value;

    /** LocalDate representation of this LongDate. Null if value is 0. */
    private final LocalDate date;

    /**
     * Constructs a LongDate from an integer value in AAAAMMDD format.
     *
     * @param value numeric date value (e.g., 20251013), or 0 for empty
     */
    private LongDate(int value) {
        this.value = value;

        if (value == 0) {
            this.date = null;
        } else {
            int year = value / 10000;
            int month = (value / 100) % 100;
            int day = value % 100;
            this.date = LocalDate.of(year, month, day);
        }
    }

    /**
     * Constructs a LongDate from a LocalDate.
     *
     * @param date LocalDate to convert, or null for empty
     * @return LongDate instance
     */
    public static LongDate fromLocalDate(LocalDate date) {
        if (date == null) {
            return new LongDate(0);
        }
        int value = date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
        return new LongDate(value);
    }

    /**
     * Creates a LongDate from a numeric AAAAMMDD value.
     *
     * @param value numeric date value, or 0 for empty
     * @return LongDate instance
     */
    public static LongDate fromValue(int value) {
        return new LongDate(value);
    }

    /**
     * Returns the hexadecimal string representation of this date.
     *
     * @return hexadecimal string (e.g., "0x135F7A5" for 20251013),
     *         or "0x0" for empty
     */
    public String toHexString() {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    /**
     * Converts this LongDate to a 2-byte representation.
     * The first byte is the most significant byte (MSB),
     * and the second byte is the least significant byte (LSB).
     *
     * <p>
     * Note: Only the lower 16 bits of the value are stored.
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
     * Returns the ISO 8601 string representation of this date for JSON serialization.
     *
     * @return ISO 8601 formatted date string, or empty string if no date
     */
    @JsonValue
    public String toJsonValue() {
        return date != null ? date.toString() : "";
    }

    /**
     * Returns true if this LongDate represents an empty date (value == 0).
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return value == 0;
    }

    /**
     * @return a {@link LongDate} with value 0
     */
    public static LongDate empty() {
        return new LongDate(0);
    }

    @Override
    public String toString() {
        return toJsonValue();
    }
}
