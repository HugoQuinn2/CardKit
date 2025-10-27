package com.idear.devices.card.cardkit.core.io.card.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a generic file stored on a card.
 * Subclasses must implement their own parsing and unparsing logic.
 *
 * @param <T> the specific subclass type extending File
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class File<T extends File<T>> extends Item {

    @JsonIgnore
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

    public File(String content, byte fileId) {
        this.content = content;
        this.fileId = fileId;
    }

    public File() {
    }
}
