package com.idear.devices.card.cardkit.core.io.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a progress update for a transaction.
 */
@Data
@AllArgsConstructor
public class ProgressUpdate {
    private int percent;
    private String message;
}
