package com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx;

import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.CalypsoProduct;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file.*;
import com.idear.devices.card.cardkit.core.io.card.Card;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Calypso applications contain files that are organized in a hierarchical structure according to the definitions
 * of ISO/IEC 7816‑4.
 *
 * <p>
 *     The transport network defines the file structure that must be implemented in Calypso applications.
 *     The structure can be one of the standardized ones by the Calypso Networks Association (CNA) (see specification
 *     Calypso 060709‐CalypsoFiles), it can be a modified version of the predefined ones, or it can be entirely new.
 * </p>
 *
 * @author Victor Hugo Gaspar Quinn
 * @version 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CalypsoCardCDMX extends Card {

    /** Is DF invalidate */
    private boolean enabled;

    /** contains the type of card product, required for the MAC signature */
    private CalypsoProduct calypsoProduct;

    /** It contains general information about the transportation application and the cardholder. */
    private Environment environment;

    /** Log of the last 3 transactions made with the card */
    private Events events = new Events();

    /** It contains the transport contracts. */
    private Contracts contracts = new Contracts();

    /** This file contains the information of the 3 most recent debit operations. The SV Debit or SV Undebit commands
     * add the transaction information to the first record of this file, as long as the transaction was successful. */
    private DebitLog debitLog;

    /** This file contains the information of the most recent recharge. The SV Reload command adds the transaction
     * information to the record in this file, as long as the transaction has been successful. */
    private LoadLog loadLog;

}
