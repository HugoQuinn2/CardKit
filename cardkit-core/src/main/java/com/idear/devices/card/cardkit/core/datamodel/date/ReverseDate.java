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

    /** Maximum number of days representable using 14 bits (2^14 - 1). */
    public static final int MAX_DAYS = 0x3FFF;

    private final LocalDate date;
    private final int value;

    /**
     * Constructs a ReverseDate from a reversed day code.
     *
     * @param days the 14-bit reversed day code
     */
    private ReverseDate(int days) {
        this.value = days;
        this.date = CompactDate.OFFSET.plusDays(days);
    }

    /**
     * Returns a ReverseDate representing the current system date.
     *
     * @return ReverseDate instance for today
     */
    public static ReverseDate now() {
        return fromLocalDate(LocalDate.now());
    }

    /**
     * Creates a ReverseDate from a day count since {@link CompactDate#OFFSET}.
     * The 14 LSB are inverted using XOR with 0x3FFF.
     *
     * @param days number of days since OFFSET
     * @return ReverseDate instance
     */
    public static ReverseDate fromDays(int days) {
        int reverseDays = days ^ MAX_DAYS;
        return new ReverseDate(reverseDays);
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
        return getValue() ^ MAX_DAYS;
    }

    /**
     * Converts this ReverseDate to a 2-byte representation.
     * The MSB (Most Significant Byte) is stored first.
     *
     * @return byte array of size 2 representing this date
     */
    public byte[] toBytes() {
        int value = getValue();
        byte msb = (byte) ((value & 0xFF00) >> 8);
        byte lsb = (byte) (value & 0x00FF);
        return new byte[]{msb, lsb};
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
}
