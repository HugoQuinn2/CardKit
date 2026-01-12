package com.idear.devices.card.cardkit.core.io.card.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.io.Item;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a generic file stored on a card.
 * Subclasses must implement their own parsing and unparsing logic.
 *
 * @param <T> the specific subclass type extending File
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class File<T extends File<T>> extends Item {

    private String content;

    @JsonIgnore
    private byte fileId;

    /**
     * Converts the file object into a byte array representation.
     *
     * @return a byte array containing the serialized data
     */
    public abstract byte[] unparse();

    /**
     * Parses the given byte array and returns a new instance of the same type.
     *
     * @param data the raw byte data to parse
     * @return a parsed object of type T
     */
    public abstract T parse(byte[] data);

    /**
     * Update the {@link File#content} unparsing the file
     */
    public void update() {
        this.content = Strings.bytesToHex(unparse());
    }

}
