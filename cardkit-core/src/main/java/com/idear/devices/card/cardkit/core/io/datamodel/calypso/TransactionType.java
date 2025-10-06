package com.idear.devices.card.cardkit.core.io.datamodel.calypso;

import com.idear.devices.card.cardkit.core.io.datamodel.IDataModel;
import lombok.Getter;

@Getter
public enum TransactionType implements IDataModel {
    /*
     * Events TO BE written in the card
     */
    RELOAD(0x00, true, true, true),
    BALANCE_RECOVERY(0x01, true, true, true),
    CLAIM_TICKET_REFUND(0x02, true, true, true),
    GENERAL_DEBIT(0x03, true, true, true),
    MULTIMODAL_FREE_PASS(0x04, true, true, true),
    SPECIAL_SERVICE_DEBIT(0x05, true, true, true),
    INTERCHANGE(0x07, true, true, true),
    BALANCE_CANCELLATION_LOAD_SAM_NOT_WHITELISTED(0x14, true, true, true),
    SERVICE_CARD(0x15, true, true, true),
    BALANCE_CANCELLATION_DEBIT_SAM_NOT_WHITELISTED(0x18, true, true, true),
    /*
     * Events NOT TO BE written in the card
     */
    CARD_PURCHASE(0x08, false, true, true),
    SV_CONTRACT_RENEWAL(0x09, false, true, true),
    INVALIDATION(0x0A, false, true, true),
    BLACKLISTED_CARD(0x0D, false, true, false),
    REENTRY(0x0E, false, true, false),
    CONTROL(0x0F, false, true, false),
    RELOAD_FAILURE(0x10, false, true, false),
    EXIT(0x11, false, true, false),
    DEBIT_ERROR_REFUND(0x12, false, true, true),
    LOAD_SAM_ID_NOT_WHITELISTED(0x13, false, true, false),
    PURCHASE_FAILURE_REFUND(0x15, false, true, true),
    PURCHASE_FAILURE(0x16, false, true, false),
    CARD_TRANSACTION_ABORTED_AND_MAC_COMPUTED(0x4E, false, true, true),
    MAC_COMPUTED_WITHOUT_CARD_MODIFICATIONS(0x4F, false, true, true),
    CARD_UNAVAILABLE(0x50, false, true, false),
    INSUFFICIENT_BALANCE(0x51, false, true, false),
    ANTIPASSBACK(0x52, false, true, false),
    EXCEEDING_SV_BALANCE(0x53, false, true, false),
    CARD_TO_PURCHASE_BALANCE_GREATER_THAN_ZERO(0x5A, false, true, false),
    INVALID_DF_STATUS(0x54, false, true, false),
    INVALID_CONTRACT_AUTHENTICATOR(0x55, false, true, false),
    EXPIRED_CARD_DATES(0x60, false, true, false),
    ABORTED_CARD_TRANSACTION(0x61, false, true, false),
    MONOMODAL_FREE_PASS(0x70, false, true, true),
    REMOTE_RELOAD_FAILURE_AND_COUNTER_INCREMENTED(0xD0, false, true, false),

    /*
     * Events not reported to the back-office nor written to the card
     * (not included in the data model)
     */
    RFU(-1, false, false, false),
    UNEXPECTED_FILE_STRUCTURE(-2, false, false, false),
    UNEXPECTED_ENV_VERSION_NUMBER(-3, false, false, false),
    UNEXPECTED_ENVIRONMENT_COUNTRY(-4, false, false, false),
    UNEXPECTED_ENVIRONMENT_NETWORK(-5, false, false, false),
    UNEXPECTED_ENVIRONMENT_ISSUER(-6, false, false, false),
    NO_CONTRACT_ALLOWS_ENTRY(-7, false, false, false),
    PSO_VERIFY_BUSY_STATUS(-8, false, false, false),
    SAM_UNAVAILABLE(-9, false, false, false),
    CARD_VERIFICATION(-10, false, false, false),
    UNEXPECTED_CARD_ERROR(-11, false, false, false),
    DISALLOWED_CONTRACT_PROVIDER(-12, false, false, false),
    SIGNING_KEY_NOT_FOUND(-13, false, false, false),
    UNEXPECTED_HOLDER_PROFILE(-14, false, false, false),
    ACCESS_RATIFIED(-15, false, false, false),
    AID_RT_CDMX_NOT_FOUND(-16, false, false, false),
    CALYPSO_HCE_NOT_ALLOWED(-17, false, false, false),
    MAX_SV_BALANCE(-18, false, false, false),
    SV_STATE_CHANGE(-19, false, false, false),
    TARIFF_CHANGE_WITH_CARD(-20, false, true, false),
    UNEXPECTED_CARD_DATES(-21, false, false, false),
    ILLEGAL_TRANSACTION_FLOW(-22, false, false, false),
    MAX_EVENT_TNUM(-23, false, false, false),
    DIFFERENT_CARD_INSERTED(-24, false, false, false),
    INVALID_SELECTION_RESPONSE(-25, false, false, false),
    CONTRACT_SALE_SAM_NOT_WHITELISTED(-26, false, false, false);

    private final int value;
    private final boolean isWritten;
    private final boolean isReported;
    private final boolean isSigned;

    TransactionType(int value, boolean isWritten, boolean isReported, boolean isSigned) {
        this.value = value;
        this.isWritten = isWritten;
        this.isReported = isReported;
        this.isSigned = isSigned;
    }

    public static TransactionType decode(int value) {
        for (TransactionType v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return RFU;
    }

    @Override
    public int getValue() {
        return value;
    }
}
