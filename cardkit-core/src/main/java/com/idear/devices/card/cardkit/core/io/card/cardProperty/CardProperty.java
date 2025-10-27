package com.idear.devices.card.cardkit.core.io.card.cardProperty;

import com.fasterxml.jackson.annotation.JsonValue;
import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
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
public class CardProperty<E extends Enum<E> & IDataModel> implements IDataModel {

    /**
     * The raw integer value of the property.
     */
    private int value;

    /**
     * The enum class used to decode the value.
     */
    private Class<E> enumClass;

    /**
     * Sets the value of this property using another {@link IDataModel} instance.
     *
     * @param iDataModel the data model whose value will be copied
     */
    public void setValueByModel(E iDataModel) {
        this.value = iDataModel.getValue();
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
    public E decodeOrElse(E defaultDecode) {
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

    /**
     * Creates a {@link CardProperty} from a string input.
     * If the input matches an enum constant name (case-insensitive), the corresponding value is used.
     * Otherwise, the input is parsed as a hexadecimal integer.
     *
     * @param input     the string to interpret (enum name or hex value)
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link CardProperty} instance
     * @throws IllegalArgumentException if the input is neither a valid enum name nor a hex value
     */
    public static <E extends Enum<E> & IDataModel> CardProperty<E> fromHexStringValue(String input, Class<E> enumClass) {
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(input)) {
                return new CardProperty<>(constant.getValue(), enumClass);
            }
        }

        try {
            int parsedValue = Integer.parseInt(input, 16);
            return new CardProperty<>(parsedValue, enumClass);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input: not a valid enum name or hex value " + input, e);
        }
    }

    /**
     * Creates an empty {@link CardProperty} instance with a default value of 0.
     * This is useful for initializing a property when no value has been assigned yet.
     *
     * @param enumClass the enum class used for decoding
     * @param <E>       the enum type that implements {@link IDataModel}
     * @return a new {@link CardProperty} instance with value 0
     */
    public static <E extends Enum<E> & IDataModel> CardProperty<E> emptyProperty(Class<E> enumClass) {
        return new CardProperty<>(0, enumClass);
    }

}
