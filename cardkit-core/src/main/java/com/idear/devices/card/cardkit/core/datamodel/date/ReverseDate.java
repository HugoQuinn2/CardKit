package com.idear.devices.card.cardkit.core.datamodel.date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a compact date format similar to {@link CompactDate},
 * but with all bits inverted in the 14 least significant bits (LSB) of the day count.
 * <p>
 * The inversion is performed using XOR (⊕) with 0x3FFF. Flipping a bit
 * from 0 to 1 moves the date towards the past relative to the original date.
 * Therefore, operations that only set bits (OR) do not introduce a security risk.
 * </p>
 *
 * <p>
 * The reference date is {@code CompactDate.OFFSET} (1997-01-01),
 * and only the 14 LSB are used, allowing a maximum of 16,383 days.
 * </p>
 *
 * <p>
 * Example usage:
 * <ul>
 *     <li>{@code ReverseDate.now()} returns the current system date in reversed-bit format.</li>
 *     <li>{@code ReverseDate.fromLocalDate(LocalDate)} creates a ReverseDate from a standard LocalDate.</li>
 *     <li>{@code unReverseDays()} returns the original day count before inversion.</li>
 * </ul>
 * </p>
 */
@Getter
public class ReverseDate extends Item implements IDataModel {

    /** Mask for the 14 least significant bits (2^14 - 1). */
    public static final int MASK_14_BITS = 0x3FFF;

    /** Reference date (1997-01-01). */
    public static final LocalDate OFFSET = LocalDate.of(1997, 1, 1);

    /** Stored inverted value (14 bits). */
    private final int value;

    /** Human-readable date computed from the non-inverted value. */
    private final LocalDate date;

    /**
     * Internal constructor from a reversed day value.
     *
     * @param reversedDays 14-bit inverted day count
     */
    private ReverseDate(int reversedDays) {
        validate14Bits(reversedDays);

        this.value = reversedDays;

        int normalDays = reversedDays ^ MASK_14_BITS;
        this.date = OFFSET.plusDays(normalDays);
    }

    /**
     * Internal constructor from a reversed day value.
     *
     * @param value 14-bit inverted day count
     */
    private ReverseDate(int value, LocalDate localDate) {
        this.value = value;
        this.date = localDate;
    }

    public static ReverseDate zero() {
        return new ReverseDate(0, null);
    }

    /**
     * Creates a {@code ReverseDate} from a normal day count since {@link #OFFSET}.
     *
     * @param days number of days since OFFSET (0–16383)
     * @return ReverseDate instance
     */
    public static ReverseDate fromDays(int days) {
        validate14Bits(days);

        int reversedDays = days ^ MASK_14_BITS;
        return new ReverseDate(reversedDays);
    }

    /**
     * Returns the non-inverted day count since {@link #OFFSET}.
     *
     * @return original day count
     */
    public int toDays() {
        return value ^ MASK_14_BITS;
    }

    /**
     * Returns a ReverseDate representing the current system date.
     *
     * @return ReverseDate instance for today
     */
    public static ReverseDate now() {
        return fromLocalDate(LocalDate.now());
    }

    public static ReverseDate fromReversedValue(int reversedDays) {
        return new ReverseDate(reversedDays);
    }

    /**
     * Creates a ReverseDate from a LocalDate.
     *
     * @param localDate the LocalDate to convert
     * @return ReverseDate instance
     */
    public static ReverseDate fromLocalDate(LocalDate localDate) {
        int days = (int) ChronoUnit.DAYS.between(CompactDate.OFFSET, localDate);
        return fromDays(days);
    }

    /**
     * Returns the original day count (un-inverted) from the stored reversed value.
     *
     * @return integer representing the original number of days since OFFSET
     */
    public int unReverseDays() {
        return getValue() ^ MASK_14_BITS;
    }

    /**
     * Converts this ReverseDate to a 2-byte representation.
     * The MSB (Most Significant Byte) is stored first.
     *
     * @return byte array of size 2 representing this date
     */
    public byte[] toBytes() {
        return new byte[]{
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    /**
     * Returns the ISO 8601 formatted representation of this date.
     *
     * @return ISO 8601 formatted date string (e.g. "2025-10-13")
     */
    @JsonValue
    public String toJsonValue() {
        return date.toString();
    }

    private static void validate14Bits(int value) {
        if (value < 0 || value > MASK_14_BITS) {
            throw new IllegalArgumentException(
                    "Value out of range (0–16383): " + value
            );
        }
    }
}
