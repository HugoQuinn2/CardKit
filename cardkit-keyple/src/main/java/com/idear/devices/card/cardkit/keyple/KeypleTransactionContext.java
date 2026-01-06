package com.idear.devices.card.cardkit.keyple;

import com.idear.devices.card.cardkit.core.io.transaction.AbstractTransactionContext;
import lombok.Builder;
import lombok.Getter;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;

@Builder
@Getter
public class KeypleTransactionContext extends AbstractTransactionContext {
    private final KeypleCardReader keypleCardReader;
    private final KeypleCalypsoSamReader keypleCalypsoSamReader;
    private final SecureRegularModeTransactionManager cardTransactionManager;
}
