package com.idear.devices.card.cardkit.core.io.card.file;

import com.idear.devices.card.cardkit.core.io.Item;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class File extends Item {
    private String content;

    public File(String content) {
        this.content = content;
    }

    public File() {
    }
}
