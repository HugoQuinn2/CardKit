package com.idear.devices.card.cardkit.core.datamodel.calypso.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idear.devices.card.cardkit.core.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.*;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.datamodel.date.CompactDate;
import com.idear.devices.card.cardkit.core.datamodel.date.ReverseDate;
import com.idear.devices.card.cardkit.core.utils.BitUtil;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import lombok.*;
import org.eclipse.keyple.core.util.HexUtil;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class Contract extends File<Contract> {

    /** Record number from list*/
    private int id;

    /** Structure version contract*/
    private int version;

    /** Actual status contract*/
    private ContractStatus status;

    /** Reserved for future use*/
    private int rfu = 0;

    /** Start date validity contract*/
    private ReverseDate startDate;

    /** Duration contract*/
    private int duration;

    /** Network that issued this contract*/
    private NetworkCode network;

    /** Provider that issued this contract*/
    private Provider provider;

    /** Modes of transportation in which the contract is valid*/
    private Modality modality;

    private int counterCode;

    /** Contract type*/
    private Tariff tariff;

    /** Determine whether the contract allows transfers*/
    private int journeyInterChanges;

    /** Code to identify the class to which the contract belongs (e.g: first class, second class, ...)
     * Defined by the {@link Contract#provider} Organization
     */
    private int vehicleClassAllowed;

    /** Code to identify temporal constraints*/
    private RestrictTime restrictTime;

    /** Code of the condition for which the contract is not valid*/
    private int restrictCode;

    /** Number of trips authorized per period in format*/
    private int periodJourney;

    /** Geographical areas where the contract is valid*/
    private long location;

    /** Contract issue date*/
    private CompactDate saleDate;
    private int saleSam;
    private int saleCounter = 0;
    private int authKvc = 0;
    private int authenticator = 0;

    public Contract(int id) {
        super(null, Calypso.CONTRACT_FILE);
        this.id = id;
    }

    public boolean isExpired(int daysOffset) {
        return LocalDate.now().isAfter(getExpirationDate(daysOffset));
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
        bit.setNextInteger(saleSam, 32);
        bit.setNextInteger(saleCounter, 24);
        bit.setNextInteger(authKvc, 8);
        bit.setNextInteger(authenticator, 24);

        setContent(HexUtil.toHex(bit.getData()));
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

        setContent(HexUtil.toHex(data));

        this.version              = data[0] & 0xff;
        this.status               = ContractStatus.decode(data[1] & 0xff);
        this.rfu                  = (data[2] & 0b11000000) >> 6;
        this.startDate            = ReverseDate.fromDays(ByteUtils.extractInt(data, 2, 2, false) & 0x3fff);
        this.duration             = data[4] & 0xff;
        this.network              = NetworkCode.decode(data[5] & 0xff);
        this.provider             = Provider.decode(data[6] & 0xff);

        this.modality             = Modality.decode((data[7] & 0b10000000) >> 7);
        this.counterCode          = (data[7] & 0b01100000) >> 5;
        this.tariff               = Tariff.decode((data[7] & 0b00011111));

        this.journeyInterChanges  = (data[8] & 0b10000000) >> 7;
        this.vehicleClassAllowed  = (data[8] & 0b01100000) >> 5;
        this.restrictTime         = RestrictTime.decode(data[8] & 0b00011111);

        this.restrictCode         = data[9] & 0xff;
        this.periodJourney        = data[10] & 0xff;
        this.location             = ByteUtils.extractLong(data, 11, 5, false);
        this.saleDate             = CompactDate.fromDays(ByteUtils.extractInt(data, 16, 2, false));
        this.saleSam              = ByteUtils.extractInt(data, 18, 4, false);
        this.saleCounter          = ByteUtils.extractInt(data, 22, 3, false);
        this.authKvc              = data[25] & 0xff;
        this.authenticator        = ByteUtils.extractInt(data, 26, 3, false);

        return this;
    }

    public LocalDate getExpirationDate(int daysOffset) {
        int _duration = duration & 0xFF;
        PeriodType period = PeriodType.decode((_duration >> 6) & 0b11);
        int trips = _duration & 0b00111111;

        switch (period) {
            case MONTH:
                return startDate.getDate().plusMonths(trips).minusDays(daysOffset);
            case WEEK:
                return startDate.getDate().plusWeeks(trips).minusDays(daysOffset);
            case DAY:
                return startDate.getDate().plusDays(trips).minusDays(daysOffset);
        }

        throw new IllegalArgumentException("Invalid duration format this most be 0bnnpppppp, n: period, p: trips");
    }

    public static Contract buildContract(
            int id,
            int version,
            NetworkCode networkCode,
            Provider provider,
            Modality modality,
            Tariff tariff,
            RestrictTime restrictTime,
            int saleSam) {
        Contract contract = new Contract(id);

        contract.setVersion(version);
        contract.setNetwork(networkCode);
        contract.setProvider(provider);
        contract.setModality(modality);
        contract.setTariff(tariff);
        contract.setRestrictTime(restrictTime);
        contract.setSaleSam(saleSam);

        contract.setStatus(ContractStatus.CONTRACT_PARTLY_USED);
        contract.setStartDate(ReverseDate.now());
        contract.setSaleDate(CompactDate.now());
        contract.setDuration(PeriodType.encode(PeriodType.MONTH, 60));

        return contract;
    }

}