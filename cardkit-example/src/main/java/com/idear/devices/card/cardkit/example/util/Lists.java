package com.idear.devices.card.cardkit.example.util;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Lists {
    private List<String> debitWhiteList;
    private List<String> loadWhiteList;
    private List<String> blackList;
}
