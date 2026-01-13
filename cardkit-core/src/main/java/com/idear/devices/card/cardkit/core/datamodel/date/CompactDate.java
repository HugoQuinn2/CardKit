package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Getter;

import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a compact date format where the date is stored
 * as the number of days since a fixed reference date (OFFSET).
 * <p>
 * The reference date (OFFSET) is set to {@code 1997-01-01}.
 * The date representation follows the ISO 8601 standard, which
 * defines an unambiguous, system-independent way to represent
 * calendar dates (YYYY-MM-DD).
 * </p>
 *
 * <p>
 * For example:
 * <ul>
 *   <li>{@code CompactDate.now()} returns today's date as a compact integer-based value.</li>
 *   <li>{@code CompactDate.fromLocalDate(LocalDate.of(2025, 1, 1))} creates a CompactDate representing
 *       the number of days since {@code 1997-01-01}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class supports conversion to and from a two-byte format,
 * where:
 * <ul>
 *   <li>MSB (Most Significant Byte) stores the upper 8 bits of the day count.</li>
 *   <li>LSB (Least Significant Byte) stores the lower 8 bits.</li>
 * </ul>
 * </p>
 *
 * @see java.time.LocalDate
 * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 Standard</a>
 */
@Getter
public class CompactDate extends Item implements IDataModel {

    /**
     * Reference date (OFFSET) used as the base for calculating days.
     * All dates are measured as days since this date.
     */
    public static final LocalDate OFFSET = LocalDate.of(1997, 1, 1);

    /** Maximum number of days representable (2 bytes → 65,535). */
    public static final int MAX_DAYS = 65_535;

    private final LocalDate date;
    private final int value;

    /**
     * Returns a CompactDate representing the current system date,
     * following the ISO 8601 local time standard.
     *
     * @return CompactDate instance representing today's date
     */
    public static CompactDate now() {
        return fromLocalDate(LocalDate.now());
    }

    /**
     * Creates a CompactDate instance from a given number of days
     * since the reference {@link #OFFSET}.
     *
     * @param days number of days since {@code 1997-01-01}
     * @return CompactDate instance
     */
    public static CompactDate fromDays(int days) {
        return new CompactDate(days);
    }

    /**
     * Creates a CompactDate instance from a LocalDate.
     * The number of days between the OFFSET and the provided date
     * is calculated and stored internally.
     *
     * @param localDate the date to convert
     * @return CompactDate instance
     */
    public static CompactDate fromLocalDate(LocalDate localDate) {
        int days = (int) ChronoUnit.DAYS.between(OFFSET, localDate);
        return new CompactDate(days);
    }

    /**
     * Constructs a CompactDate with the given number of days since the OFFSET.
     *
     * @param days number of days since {@code 1997-01-01}
     * @throws IllegalArgumentException if the number of days exceeds {@link #MAX_DAYS}
     */
    private CompactDate(int days) {
        if (days > MAX_DAYS)
            throw new IllegalArgumentException("Compact date days cannot be greater than " + MAX_DAYS);
        this.value = days;
        this.date = OFFSET.plusDays(days);
    }

    /**
     * Converts this CompactDate to a 2-byte representation.
     * The MSB (Most Significant Byte) is stored first.
     *
     * @return byte array of size 2 representing this date
     */
    public byte[] toBytes() {
        byte msb = (byte) ((getValue() & 0xFF00) >> 8);
        byte lsb = (byte) (getValue() & 0x00FF);
        return new byte[]{msb, lsb};
    }

    /**
     * Checks whether this CompactDate represents the OFFSET date (empty state).
     *
     * @return {@code true} if the date equals the OFFSET, {@code false} otherwise
     */
    public boolean isEmpty() {
        return getDate().equals(OFFSET);
    }

    /**
     * Returns the ISO 8601 formatted representation of this date.
     *
     * @return ISO 8601 formatted date string (e.g. "2025-10-13")
     */
    @JsonValue
    public String toJsonValue() {
        return isEmpty() ? "" : date.toString();
    }

    @Override
    public String toString() {
        return toJsonValue();
    }

    /**
     * @return returns a {@link CompactDate} with value 0
     */
    public static CompactDate empty() {
        return new CompactDate(0);
    }
}
