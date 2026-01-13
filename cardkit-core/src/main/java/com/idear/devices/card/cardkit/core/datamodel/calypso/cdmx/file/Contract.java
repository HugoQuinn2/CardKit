package com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.datamodel.decoder.ValueDecoder;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.Calypso;
import com.idear.devices.card.cardkit.core.datamodel.calypso.cdmx.constant.*;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.core.utils.Strings;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class Contract extends File<Contract> {

    /** Record number from list*/
    private int id;

    /** Structure version contract*/
    private final ValueDecoder<Version> version = ValueDecoder.emptyDecoder(Version.class);

    /** Actual status contract*/
    private final ValueDecoder<ContractStatus> status = ValueDecoder.emptyDecoder(ContractStatus.class);

    /** Reserved for future use*/
    private int rfu = 0;

    /** Start date validity contract*/
    private ReverseDate startDate;

    /** Duration contract*/
    private int duration;

    /** Network that issued this contract*/
    private final ValueDecoder<NetworkCode> network = ValueDecoder.emptyDecoder(NetworkCode.class);

    /** Provider that issued this contract*/
    private final ValueDecoder<Provider> provider = ValueDecoder.emptyDecoder(Provider.class);

    /** Modes of transportation in which the contract is valid*/
    private final ValueDecoder<Modality> modality = ValueDecoder.emptyDecoder(Modality.class);

    private int counterCode;

    /** Contract type*/
    private final ValueDecoder<Tariff> tariff = ValueDecoder.emptyDecoder(Tariff.class);

    /** Determine whether the contract allows transfers*/
    private int journeyInterChanges;

    /** Code to identify the class to which the contract belongs (e.g: first class, second class, ...)
     * Defined by the {@link Contract#provider} Organization
     */
    private int vehicleClassAllowed;

    /** Code to identify temporal constraints*/
    private final ValueDecoder<RestrictTime> restrictTime = ValueDecoder.emptyDecoder(RestrictTime.class);

    /** Code of the condition for which the contract is not valid*/
    private int restrictCode;

    /** Number of trips authorized per period in format*/
    private int periodJourney;

    /** Geographical areas where the contract is valid*/
    private long location;

    /** Contract issue date*/
    private CompactDate saleDate;
    private String saleSam;
    private int saleCounter = 0;
    private int authKvc = 0;
    private int authenticator = 0;

    public Contract(int id) {
        super(null, Calypso.CONTRACT_FILE);
        this.id = id;
    }

    public boolean isExpired(int daysOffset) {
        return LocalDate.now().isAfter(getExpirationDate().minusDays(daysOffset));
    }

    @JsonIgnore
    @Override
    public byte[] unparse() {
        BitUtil bit = new BitUtil(Calypso.RECORD_SIZE * 8);

        bit.setNextInteger(version, 8);
        bit.setNextInteger(status.getValue(), 8);
        bit.setNextInteger(rfu, 2);
        bit.setNextInteger(startDate.getValue(), 14);
        bit.setNextInteger(duration, 8);
        bit.setNextInteger(network.getValue(), 8);
        bit.setNextInteger(provider.getValue(), 8);
        bit.setNextInteger(modality.getValue(), 1);
        bit.setNextInteger(counterCode, 2);
        bit.setNextInteger(tariff.getValue(), 5);
        bit.setNextInteger(journeyInterChanges, 1);
        bit.setNextInteger(vehicleClassAllowed, 2);
        bit.setNextInteger(restrictTime.getValue(), 5);
        bit.setNextInteger(restrictCode, 8);
        bit.setNextInteger(periodJourney, 8);
        bit.setNextLong(location, 40);
        bit.setNextInteger(saleDate.getValue(), 16);
        bit.setNextInteger((int) Long.parseLong(saleSam, 16), 32);
        bit.setNextInteger(saleCounter, 24);
        bit.setNextInteger(authKvc, 8);
        bit.setNextInteger(authenticator, 24);

        setContent(Strings.bytesToHex(bit.getData()));
        return bit.getData();
    }

    @Override
    public Contract parse(byte[] data) {
        if (data == null)
            throw new IllegalArgumentException("Null data.");

        if (data.length > Calypso.RECORD_SIZE)
            throw new IllegalArgumentException("Data overflow.");

        if (data.length < Calypso.RECORD_SIZE) {
            byte[] tmp = new byte[Calypso.RECORD_SIZE];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }

        setContent(Strings.bytesToHex(data));

        this.version.setValue(data[0] & 0xff);
        this.status.setValue(data[1] & 0xff);
        this.rfu                  = (data[2] & 0b11000000) >> 6;
        this.startDate            = ReverseDate.fromReversedValue(ByteUtils.extractInt(data, 2, 2, false));
        this.duration             = data[4] & 0xff;
        this.network.setValue(data[5] & 0xff);
        this.provider.setValue(data[6] & 0xff);

        this.modality.setValue((data[7] & 0b10000000) >> 7);
        this.counterCode          = (data[7] & 0b01100000) >> 5;
        this.tariff.setValue((data[7] & 0b00011111));

        this.journeyInterChanges  = (data[8] & 0b10000000) >> 7;
        this.vehicleClassAllowed  = (data[8] & 0b01100000) >> 5;
        this.restrictTime.setValue(data[8] & 0b00011111);

        this.restrictCode         = data[9] & 0xff;
        this.periodJourney        = data[10] & 0xff;
        this.location             = ByteUtils.extractLong(data, 11, 5, false);
        this.saleDate             = CompactDate.fromDays(ByteUtils.extractInt(data, 16, 2, false));
        this.saleSam              = String.format("%08X", ByteUtils.extractInt(data, 18, 4, false));
        this.saleCounter          = ByteUtils.extractInt(data, 22, 3, false);
        this.authKvc              = data[25] & 0xff;
        this.authenticator        = ByteUtils.extractInt(data, 26, 3, false);

        return this;
    }

    @JsonIgnore
    public LocalDate getExpirationDate() {
       return PeriodType.getExpirationDate(startDate, duration);
    }

    public static Contract buildContract(
            int id,
            int networkCode,
            int provider,
            int modality,
            int tariff,
            int restrictTime,
            String saleSam) {
        Contract contract = new Contract(id);


        contract.getNetwork().setValue(networkCode);
        contract.getProvider().setValue(provider);
        contract.getModality().setValue(modality);
        contract.getTariff().setValue(tariff);
        contract.getRestrictTime().setValue(restrictTime);
        contract.setSaleSam(saleSam);

        contract.getVersion().setValue(Version.VERSION_3_3);
        contract.getStatus().setValue(ContractStatus.CONTRACT_PARTLY_USED);
        contract.setStartDate(ReverseDate.now());
        contract.setSaleDate(CompactDate.now());
        contract.setDuration(60);

        return contract;
    }

}