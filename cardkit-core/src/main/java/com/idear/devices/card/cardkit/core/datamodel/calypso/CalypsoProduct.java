package com.idear.devices.card.cardkit.core.datamodel.calypso;

import com.idear.devices.card.cardkit.core.datamodel.IDataModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;

@RequiredArgsConstructor
@Getter
public enum CalypsoProduct implements IDataModel {
    CALYPSO_HCE(0),
    CALYPSO_PRIME(1),
    CALYPSO_LIGHT(2),
    CALYPSO_BASIC(3),
    RFU(-1);

    private final int value;

    public static CalypsoProduct parseByCalypsoCard(CalypsoCard calypsoCard) {
        if (calypsoCard.isHce())
            return CalypsoProduct.CALYPSO_HCE;

        switch (calypsoCard.getProductType()) {
            case PRIME_REVISION_1:
            case PRIME_REVISION_2:
            case PRIME_REVISION_3:
                return CalypsoProduct.CALYPSO_PRIME;
            case LIGHT:
                return CalypsoProduct.CALYPSO_LIGHT;
            case BASIC:
                return CalypsoProduct.CALYPSO_BASIC;
            case UNKNOWN:
            default:
                return CalypsoProduct.RFU;
        }
    }

    public static CalypsoProduct decode(int value) {
        for (CalypsoProduct v : values()) {
            if (v.value ==  value)
                return v;
        }
        return RFU;
    }
}
