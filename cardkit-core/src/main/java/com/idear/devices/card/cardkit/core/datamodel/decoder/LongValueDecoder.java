package com.idear.devices.card.cardkit.core.datamodel.decoder;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LongValueDecoder<E extends Enum<E> & IDataModel> {
    /**
     * The raw integer value of the property.
     */
    private long value;

    /**
     * The enum class used to decode the value.
     */
    private Class<E> enumClass;

    /**
     * The default value used on case of empty value
     */
    private long defaultValue;

    /**
     * Sets the value of this property.
     *
     * @param value the data whose value will be copied
     */
    public void setValue(long value) {
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
     * Get {@link LongValueDecoder#value} if this is different to 0, otherwise get {@link LongValueDecoder#defaultValue }
     */
    public long getValue() {
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
            return Long.toHexString(value);
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
    public static <E extends Enum<E> & IDataModel> LongValueDecoder<E> fromHexStringValue(String input, Class<E> enumClass, long defaultValue) {
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(input)) {
                return new LongValueDecoder<>(constant.getValue(), enumClass, defaultValue);
            }
        }

        try {
            int parsedValue = Integer.parseInt(input, 16);
            return new LongValueDecoder<>(parsedValue, enumClass, defaultValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input: not a valid enum name or hex value " + input, e);
        }
    }

    /**
     * Creates a {@link LongValueDecoder} from a string input.
     * If the input matches an enum constant name (case-insensitive), the corresponding value is used.
     * Otherwise, the input is parsed as a hexadecimal integer.
     *
     * @param input     the string to interpret (enum name or hex value)
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link LongValueDecoder} instance
     * @throws IllegalArgumentException if the input is neither a valid enum name nor a hex value
     */
    public static <E extends Enum<E> & IDataModel> LongValueDecoder<E> fromHexStringValue(String input, Class<E> enumClass) {
        return fromHexStringValue(input, enumClass, 0);
    }

    /**
     * Creates an empty {@link LongValueDecoder} instance with a default value of 0.
     * This is useful for initializing a property when no value has been assigned yet.
     *
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link LongValueDecoder} instance with value 0
     */
    public static <E extends Enum<E> & IDataModel> LongValueDecoder<E> emptyDecoder(Class<E> enumClass) {
        return emptyDecoder(enumClass, 0);
    }

    /**
     * Creates an empty {@link LongValueDecoder} instance with a default value of 0.
     * This is useful for initializing a property when no value has been assigned yet.
     *
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link LongValueDecoder} instance with value 0
     */
    public static <E extends Enum<E> & IDataModel> LongValueDecoder<E> emptyDecoder(Class<E> enumClass, int defaultValue) {
        return new LongValueDecoder<>(0, enumClass, defaultValue);
    }
}
