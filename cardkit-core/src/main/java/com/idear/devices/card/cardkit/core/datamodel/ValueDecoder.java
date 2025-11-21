package com.idear.devices.card.cardkit.core.datamodel;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a property backed by an integer value that can optionally be decoded into a known enum constant.
 * If the value matches a constant in the provided enum class, it can be resolved to that constant.
 * Otherwise, the raw integer value is preserved.
 *
 * @param <E> the enum type that implements {@link IDataModel}
 * @version 1.0.0
 * @author Victor Hugo Gaspar Quinn
 */
@AllArgsConstructor
@Data
public class ValueDecoder<E extends Enum<E> & IDataModel> implements IDataModel {

    /**
     * The raw integer value of the property.
     */
    private int value;

    /**
     * The enum class used to decode the value.
     */
    private Class<E> enumClass;

    /**
     * The default value used on case of empty value
     */
    private int defaultValue;

    /**
     * Sets the value of this property.
     *
     * @param value the data whose value will be copied
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Sets the value of this property using another {@link IDataModel} instance.
     *
     * @param iDataModel the data model whose value will be copied
     */
    public void setValue(E iDataModel) {
        setValue(iDataModel.getValue());
    }

    /**
     * Get {@link ValueDecoder#value} if this is different to 0, otherwise get {@link ValueDecoder#defaultValue }
     */
    public int getValue() {
        return value == 0 ?
                defaultValue : value;
    }

    /**
     * Attempts to decode the current value into a corresponding enum constant.
     *
     * @return the matching enum constant
     * @throws IllegalArgumentException if no matching constant is found
     */
    public E decode() {
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.getValue() == value)
                return constant;
        }

        throw new IllegalArgumentException(
                String.format(
                        "decode fail, value %s could not be encoded with %s",
                        value,
                        enumClass
                )
        );
    }

    /**
     * Attempts to decode the current value into a corresponding enum constant.
     * Returns the provided default if decoding fails.
     *
     * @param defaultDecode the fallback enum constant
     * @return the decoded enum constant or the fallback
     */
    public E decode(E defaultDecode) {
        try {
            return decode();
        } catch (Exception e) {
            return defaultDecode;
        }
    }

    /**
     * Serializes the property to a JSON-compatible string.
     * If the value matches an enum constant, its name is returned.
     * Otherwise, the value is returned as a hexadecimal string.
     *
     * @return the JSON representation of the property
     */
    @JsonValue
    public String toJsonValue() {
        try {
            return decode().name();
        } catch (Exception e) {
            return Integer.toHexString(value);
        }
    }

    @Override
    public String toString() {
        return toJsonValue();
    }

    /**
     * Creates a {@link ValueDecoder} from a string input.
     * If the input matches an enum constant name (case-insensitive), the corresponding value is used.
     * Otherwise, the input is parsed as a hexadecimal integer.
     *
     * @param input     the string to interpret (enum name or hex value)
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link ValueDecoder} instance
     * @throws IllegalArgumentException if the input is neither a valid enum name nor a hex value
     */
    public static <E extends Enum<E> & IDataModel> ValueDecoder<E> fromHexStringValue(String input, Class<E> enumClass, int defaultValue) {
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(input)) {
                return new ValueDecoder<>(constant.getValue(), enumClass, defaultValue);
            }
        }

        try {
            int parsedValue = Integer.parseInt(input, 16);
            return new ValueDecoder<>(parsedValue, enumClass, defaultValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input: not a valid enum name or hex value " + input, e);
        }
    }

    /**
     * Creates a {@link ValueDecoder} from a string input.
     * If the input matches an enum constant name (case-insensitive), the corresponding value is used.
     * Otherwise, the input is parsed as a hexadecimal integer.
     *
     * @param input     the string to interpret (enum name or hex value)
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link ValueDecoder} instance
     * @throws IllegalArgumentException if the input is neither a valid enum name nor a hex value
     */
    public static <E extends Enum<E> & IDataModel> ValueDecoder<E> fromHexStringValue(String input, Class<E> enumClass) {
        return fromHexStringValue(input, enumClass, 0);
    }

    /**
     * Creates an empty {@link ValueDecoder} instance with a default value of 0.
     * This is useful for initializing a property when no value has been assigned yet.
     *
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link ValueDecoder} instance with value 0
     */
    public static <E extends Enum<E> & IDataModel> ValueDecoder<E> emptyDecoder(Class<E> enumClass) {
        return emptyDecoder(enumClass, 0);
    }

    /**
     * Creates an empty {@link ValueDecoder} instance with a default value of 0.
     * This is useful for initializing a property when no value has been assigned yet.
     *
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link ValueDecoder} instance with value 0
     */
    public static <E extends Enum<E> & IDataModel> ValueDecoder<E> emptyDecoder(Class<E> enumClass, int defaultValue) {
        return new ValueDecoder<>(0, enumClass, defaultValue);
    }

}
