package com.idear.devices.card.cardkit.core.datamodel.location;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.Equipment;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the Location Code (24-bit field) for Calypso Prime cards
 * used in Mexico City's integrated transport systems.
 *
 * <p>The encoding structure depends on the service type:
 * <ul>
 *   <li>STC, MB, STE – Structured in lines and stations.</li>
 *   <li>RTP – Structured by routes.</li>
 *   <li>MB (Line 4 and Line 7) – Special binary layouts per line.</li>
 *   <li>STE – Trolleybus and Cablebús use custom binary identifiers.</li>
 * </ul>
 *
 * <p>Each Location Code is represented by a 24-bit integer:
 * <pre>
 *  Bits:  0-3   → Line
 *          4-11  → Station or Bus number
 *          12-15 → Equipment type
 *          16-23 → Device ID within station or bus
 * </pre>
 *
 * <p>Example (STC/Metro): 0xAABBCCDD</p>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class LocationCode extends Item implements IDataModel {

    /** Encoded 24-bit location value. */
    private int value;

    /**
     * Returns the hexadecimal string representation of this location code.
     *
     * @return Hex string of the encoded value.
     * @throws IllegalStateException if value is null.
     */
    @JsonValue
    public String toJsonValue() {
        return String.format("%02X", value).toUpperCase();
    }

    /**
     * Extracts the line number (first 4 bits).
     *
     * @return Line number.
     * @throws IllegalStateException if value is null.
     */
    public int getLine() {
        ensureValue();
        return (value >> 20) & 0xF;
    }

    /**
     * Extracts the station or route identifier (bits 8–15).
     *
     * @return Station or route ID.
     * @throws IllegalStateException if value is null.
     */
    public int getStationOrRoute() {
        ensureValue();
        return (value >> 12) & 0xFF;
    }

    /**
     * Extracts the Equipment type (bits 4–7) and decodes it into {@link Equipment}.
     *
     * @return Equipment type.
     * @throws IllegalStateException if value is null.
     */
    public Equipment getEquipment() {
        ensureValue();
        int equipmentBits = (value >> 8) & 0xF;
        return Equipment.decode(equipmentBits);
    }

    /**
     * Extracts the device identifier (last 8 bits).
     *
     * @return Device ID inside the station or bus.
     * @throws IllegalStateException if value is null.
     */
    public int getDeviceId() {
        ensureValue();
        return value & 0xFF;
    }

    /**
     * Checks if the location code is empty (zero value).
     *
     * @return True if value is zero.
     */
    public boolean isEmpty() {
        return value == 0;
    }

    /**
     * Creates an empty location code (value = 0).
     *
     * @return New empty LocationCode.
     */
    public static LocationCode emptyLocationCode() {
        return new LocationCode(0);
    }

    /**
     * Internal validator to ensure value is not null.
     *
     * @throws IllegalStateException if value is null.
     */
    private void ensureValue() {
        if (isEmpty())
            throw new IllegalStateException("LocationCode value cannot be null.");
    }
}
