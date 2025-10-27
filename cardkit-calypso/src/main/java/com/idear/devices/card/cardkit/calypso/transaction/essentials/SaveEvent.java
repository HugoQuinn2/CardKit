package com.idear.devices.card.cardkit.calypso.transaction.essentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.calypso.CalypsoCardCDMX;
import com.idear.devices.card.cardkit.calypso.GenericApduResponse;
import com.idear.devices.card.cardkit.calypso.ReaderPCSC;
import com.idear.devices.card.cardkit.calypso.TransactionDataEvent;
import com.idear.devices.card.cardkit.calypso.file.Contract;
import com.idear.devices.card.cardkit.calypso.file.DebitLog;
import com.idear.devices.card.cardkit.calypso.file.Event;
import com.idear.devices.card.cardkit.calypso.file.LoadLog;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.date.DateTimeReal;
import com.idear.devices.card.cardkit.core.datamodel.location.LocationCode;
import com.idear.devices.card.cardkit.core.exception.ReaderException;
import com.idear.devices.card.cardkit.core.io.transaction.Transaction;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionResult;
import com.idear.devices.card.cardkit.core.io.transaction.TransactionStatus;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.keyple.card.generic.CardTransactionManager;
import org.eclipse.keyple.card.generic.TransactionException;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.SvDebitLogRecord;
import org.eclipse.keypop.calypso.card.card.SvLoadLogRecord;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SvAction;
import org.eclipse.keypop.calypso.card.transaction.SvOperation;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a Calypso transaction that saves an event record on a card.
 * <p>
 * The {@code SaveEvent} class builds an {@link Event} instance using transaction details
 * such as the type, environment, contract, passenger, and location.
 * It then optionally fires a card event and writes the record onto the card if
 * the transaction type requires it.
 * </p>
 *
 * <p>This class extends {@link Transaction} with a Boolean result type,
 * indicating success or failure of the operation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * SaveEvent saveEvent = new SaveEvent(
 *         TransactionType.DEBIT,
 *         environment,
 *         contract,
 *         passengerId,
 *         locationId,
 *         amount,
 *         transactionNumber);
 *
 * TransactionResult&lt;Boolean&gt; result = saveEvent.execute(reader);
 * </pre>
 *
 * @author Hugo
 * @since 1.0
 */
@Getter
public class SaveEvent extends Transaction<Boolean, ReaderPCSC> {

    private final CalypsoCardCDMX calypsoCardCDMX;

    /** The type of transaction being executed (e.g., RELOAD, GENERAL_DEBIT, etc.). */
    private final TransactionType transactionType;

    /** The sequential transaction number associated with this event. */
    private final int transactionNumber;

    /** The contract file associated with the transaction. */
    private final Contract contract;
    private final NetworkCode networkCode;
    private final Provider provider;

    /** The passenger identifier related to the event. */
    private final int passenger;

    /** The identifier of the location where the event occurs. */
    private final LocationCode locationId;

    /** The amount involved in the transaction. */
    private final int amount;

    @JsonIgnore
    @ToString.Exclude
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Constructs a new {@code SaveEvent} instance.
     *
     * @param transactionType  the type of the transaction
     * @param passenger        the passenger identifier
     * @param locationId       the ID of the location where the event occurs
     * @param amount           the transaction amount
     * @param transactionNumber the sequential number of this transaction
     */
    public SaveEvent(
            CalypsoCardCDMX calypsoCardCDMX,
            TransactionType transactionType,
            NetworkCode networkCode,
            Provider provider,
            LocationCode locationId,
            Contract contract,
            int passenger,
            int transactionNumber,
            int amount) {
        super("SAVE_EVENT");
        this.calypsoCardCDMX = calypsoCardCDMX;
        this.transactionType = transactionType;
        this.passenger = passenger;
        this.locationId = locationId;
        this.amount = amount;
        this.transactionNumber = transactionNumber;
        this.contract = contract;
        this.networkCode = networkCode;
        this.provider = provider;
    }

    /**
     * Executes the event-saving process using the provided {@link ReaderPCSC}.
     * <p>
     * The method performs the following steps:
     * <ul>
     *     <li>Verifies that a card is present in the reader.</li>
     *     <li>Builds an {@link Event} object with the given transaction data.</li>
     *     <li>Triggers a card event if the transaction type is reportable.</li>
     *     <li>If writable, opens a secure session and appends the event record to the card.</li>
     * </ul>
     * </p>
     *
     * @param reader the reader used to communicate with the Calypso card
     * @return a {@link TransactionResult} containing the operation status and result
     * @throws ReaderException if no card is detected in the reader
     */
    @Override
    public TransactionResult<Boolean> execute(ReaderPCSC reader) {
        if (!reader.isCardOnReader())
            throw new ReaderException("no card on reader");

        // Build the event data
        Event event = Event.builEvent(
                transactionType,
                networkCode,
                provider,
                contract.getId(),
                passenger,
                transactionNumber,
                locationId,
                amount
        );

        // Trigger the event on the reader if the transaction is reportable
        if (transactionType.isReported()) {
            String mac;
            if (transactionType.isSigned())
                mac = computeTransactionSignature(
                        reader,
                        transactionType.getValue(),
                        DateTimeReal.now().getValue(),
                        amount,
                        locationId.getValue(),
                        0,
                        calypsoCardCDMX.getSerial(),
                        calypsoCardCDMX.getBalance(),
                        provider.getValue()
                );
            else {
                mac = "";
            }

            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.KEEP_OPEN);

            executor.submit(() ->
                    reader.fireCardEvent(
                            TransactionDataEvent
                                    .builder()
                                    .mac(mac)
                                    .debitLog(readDebitLog(reader))
                                    .loadLog(readLoadLog(reader))
                                    .event(event)
                                    .contract(contract)
                                    .profile(calypsoCardCDMX.getEnvironment().getProfile())
                                    .transactionAmount(amount)
                                    .balanceBeforeTransaction(calypsoCardCDMX.getBalance())
                                    .locationCode(locationId)
                                    .build())
            );

        }

        // Write the event to the card if required
        if (transactionType.isWritten()) {
            reader.getCardTransactionManager()
                    .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                    .prepareAppendRecord(event.getFileId(), event.unparse())
                    .prepareCloseSecureSession()
                    .processCommands(ChannelControl.CLOSE_AFTER);
        }

        return TransactionResult.<Boolean>builder()
                .transactionStatus(TransactionStatus.OK)
                .data(true)
                .message(String.format("event %s successfully save, reported: %s, written: %s",
                        transactionType,
                        transactionType.isReported(),
                        transactionType.isWritten()))
                .build();
    }

    private DebitLog readDebitLog(ReaderPCSC reader) {
        SvDebitLogRecord debitLogRecord = reader.getCalypsoCard().getSvDebitLogLastRecord();
        return new DebitLog().parse(debitLogRecord);
    }

    private LoadLog readLoadLog(ReaderPCSC reader) {
        SvLoadLogRecord loadLogRecord = reader.getCalypsoCard().getSvLoadLogRecord();
        return new LoadLog().parse(loadLogRecord);
    }

    protected String computeTransactionSignature(
            ReaderPCSC readerPCSC,
            int eventType, int transactionTimestamp, int transactionAmount,
            int terminalLocation, int cardType, String cardSerialHex,
            int prevSvBalance, int svProvider) {
        BitUtil bit = new BitUtil(0x20 * 8);
        bit.setNextInteger(eventType, 8);
        bit.setNextInteger(transactionTimestamp, 32);
        bit.setNextInteger(Math.abs(transactionAmount), 32);
        bit.setNextInteger(terminalLocation, 32);
        bit.setNextInteger(cardType, 8);
        bit.setNextHexaString(cardSerialHex, 64);
        bit.setNextInteger(prevSvBalance, 32);
        bit.setNextInteger(svProvider, 8);
        bit.setNextInteger(0, 16);
        bit.setNextInteger(0, 24);

        GenericApduResponse response = digestMacCompute(
                readerPCSC.getCalypsoSam().getGenericSamTransactionManager(),
                (byte) 0xEB,
                (byte) 0xC0,
                bit.getData());

        return HexUtil.toHex(response.getDataOut());
    }

    private GenericApduResponse digestMacCompute(
            CardTransactionManager samGenericTransactionManager,
            byte kif, byte kvc, byte[] data) {
        byte cla = (byte) 0x80;
        byte ins = (byte) 0x8F;
        byte p1  = (byte) 0x00;
        byte p2  = (byte) 0x00;
        byte lc  = (byte) 0x22;

        byte[] head = {cla, ins, p1, p2, lc, kif, kvc};
        byte[] apdu = new byte[head.length + data.length];
        System.arraycopy(head, 0, apdu, 0, head.length);
        System.arraycopy(data, 0, apdu, head.length, data.length);

        byte[] mac;
        String sw = "";
        try {
            byte[] response = samGenericTransactionManager
                    .prepareApdu(apdu)
                    .processApdusToByteArrays(org.eclipse.keyple.card.generic.ChannelControl.KEEP_OPEN)
                    .get(0);
            mac = Arrays.copyOfRange(response, 0, response.length - 2);
            sw = org.eclipse.keyple.core.util.HexUtil.toHex(Arrays.copyOfRange(
                    response, response.length-2, response.length));
        } catch (TransactionException e) {
            mac = new byte[0];
        }
        return new GenericApduResponse(mac, sw);
    }
}
