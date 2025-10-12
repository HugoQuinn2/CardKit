package com.idear.devices.card.cardkit.calypso.transaction;

import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.transaction.essentials.SaveEvent;
import com.idear.devices.card.cardkit.core.datamodel.calypso.ContractStatus;
import com.idear.devices.card.cardkit.core.datamodel.calypso.TransactionType;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactTime;
import com.idear.devices.card.cardkit.core.exception.CardException;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import lombok.Getter;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

/**
 * Transaction that performs a debit operation on a Calypso card.
 * <p>
 * If the debit amount exceeds {@link #MAX_POSSIBLE_AMOUNT}, multiple debit
 * operations are executed until the full amount is deducted or the card balance reaches zero.
 */
@Getter
public class DebitCard extends Transaction<Boolean, ReaderPCSC> {

    private static final int MAX_POSSIBLE_AMOUNT = 32767;

    private final CalypsoCardCDMX calypsoCardCDMX;
    private final Contract contract;
    private final int locationId;
    private int amount;
    private int contractDaysOffset = 15;
    private int provider = 0;

    /**
     * Creates a new debit transaction.
     *
     * @param calypsoCardCDMX The Calypso card instance.
     * @param contract        The contract associated with the card.
     * @param amount          The debit amount.
     * @param locationId      The location identifier.
     */
    public DebitCard(CalypsoCardCDMX calypsoCardCDMX, Contract contract, int amount, int locationId) {
        super("debit card");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.contract = contract;
        this.amount = amount;
        this.locationId = locationId;
    }

    /**
     * Executes the debit operation.
     * <p>
     * Handles multi-debit when the total amount exceeds the maximum allowed per transaction.
     *
     * @param reader The card reader.
     * @return The result of the transaction.
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        reportProgress(0, "Starting debit transaction");

        if (amount > MAX_POSSIBLE_AMOUNT)
            throw new CardException("The amount debit cannot be greater than %s, actual: %s", MAX_POSSIBLE_AMOUNT, amount);

        // --- Step 1: Card presence and verification ---
        if (!reader.getCardReader().isCardPresent()) {
            throw new ReaderException("No card on reader");
        }

        String cardSerial = HexUtil.toHex(reader.getCalypsoCard().getApplicationSerialNumber());
        reader.updateCalypsoCardSession();

        if (!cardSerial.equals(calypsoCardCDMX.getSerial())) {
            throw new CardException("Invalid card: expected %s, got %s", calypsoCardCDMX.getSerial(), cardSerial);
        }

        reportProgress(10, "Card verified");

        // --- Step 2: Contract and balance validation ---
        if (!contract.getStatus().equals(ContractStatus.CONTRACT_PARTLY_USED)
                && !contract.getStatus().equals(ContractStatus.CONTRACT_TO_BE_RENEWED)) {
            throw new CardException("No valid contract on card");
        }

        if (calypsoCardCDMX.getBalance() - amount < 0) {
            throw new CardException("Insufficient balance for debit %s, actual balance: %s",
                    amount, calypsoCardCDMX.getBalance());
        }

        reportProgress(20, "Contract validated and balance checked");

        // --- Step 3: Renew contract if needed ---
        if (contract.isExpired(contractDaysOffset)) {
            throw new CardException("Contract expired");
        }

        reportProgress(40, "Contract renewed");

        // --- Step 4: Perform multi-debit if needed ---
        int remainingAmount = amount;
        int totalDebited = 0;
        int step = 50;

        while (remainingAmount > 0 && calypsoCardCDMX.getBalance() > 0) {
            int debitChunk = Math.min(remainingAmount, MAX_POSSIBLE_AMOUNT);

            // Execute single debit
            reportProgress(step, String.format("Debiting %d units...", debitChunk));
            performDebit(reader, debitChunk);

            // Update counters
            remainingAmount -= debitChunk;
            totalDebited += debitChunk;
            step = Math.min(step + 10, 90);

            // Refresh card balance (simulate update from card)
            calypsoCardCDMX.setBalance(calypsoCardCDMX.getBalance() - debitChunk);

            if (calypsoCardCDMX.getBalance() <= 0) {
                break;
            }
        }

        reportProgress(100, "Debit operation completed successfully");

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message(String.format("Final card balance for '%s': %s (Total debited: %s)",
                        calypsoCardCDMX.getSerial(),
                        calypsoCardCDMX.getBalance(),
                        totalDebited))
                .build();
    }

    /**
     * Performs a single debit operation on the card.
     *
     * @param reader The reader instance.
     * @param debitAmount The amount to debit.
     */
    private void performDebit(ReaderPCSC reader, int debitAmount) {
        reader.getCardTransactionManager()
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                .processCommands(ChannelControl.KEEP_OPEN);

        reader.getCardTransactionManager()
                .prepareSvDebit(
                        debitAmount,
                        CompactDate.toBytes(CompactDate.now().getCode()),
                        CompactTime.toBytes(CompactTime.now().getCode())
                );

        reader.execute(new SaveEvent(TransactionType.GENERAL_DEBIT, locationId, amount));

        reader.getCardTransactionManager()
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER);
    }

    public DebitCard contractDaysOffset(int contractDaysOffset) {
        this.contractDaysOffset = contractDaysOffset;
        return this;
    }

    public DebitCard provider(int provider) {
        this.provider = provider;
        return this;
    }
}
