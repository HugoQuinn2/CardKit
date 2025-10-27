package com.idear.devices.card.cardkit.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Util {

    public static Lists loadLists() {
        try (InputStream inputStream = Util.class.getResourceAsStream("/lists.json")){

            if (inputStream == null)
                throw new NullPointerException("Lists not founded");

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    inputStream,
                    Lists.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
