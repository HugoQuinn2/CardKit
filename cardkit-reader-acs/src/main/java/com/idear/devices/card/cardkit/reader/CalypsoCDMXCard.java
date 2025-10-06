package com.idear.devices.card.cardkit.reader;

import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.io.datamodel.calypso.*;
import com.idear.devices.card.cardkit.core.utils.Assert;
import com.idear.devices.card.cardkit.core.utils.ByteUtils;
import com.idear.devices.card.cardkit.reader.file.DebitLog;
import com.idear.devices.card.cardkit.reader.file.LoadLog;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.keyple.core.util.HexUtil;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class CalypsoCDMXCard {

    public static final int RECORD_SIZE = 29;

    private String serial = "";
    private int balance = 0;
    private Environment environment;
    private List<Event> events = new ArrayList<>();
    private Contracts contracts = new Contracts();
    private DebitLog debitLog;
    private LoadLog loadLog;

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Environment extends File {

        private Version version;
        private Country country;
        private NetworkCode network;
        private int issuer;
        private int application;
        private int issuingDate;
        private int endDate;
        private int holderBirthDate;
        private int holderCompany;
        private int holderId;

        private Profile profile;

        private int prof1Date;
        private int prof2Date;
        private int prof3Date;

        private int holderPadding;

        public Environment(byte[] env) {
            super(HexUtil.toHex(env));

            if (env == null)
                throw new IllegalArgumentException("Null data.");

            if (env.length > RECORD_SIZE)
                throw new IllegalArgumentException("Data overflow.");

            if (env.length < RECORD_SIZE) {
                byte[] tmp = new byte[RECORD_SIZE];
                System.arraycopy(env, 0, tmp, 0, env.length);
                env = tmp;
            }

            this.version = Version.decode(ByteUtils.mostSignificantNibble(env[0]));
            this.country = Country.decode(ByteUtils.extractInt(env, 0, 2, false) & 0x0FFF);
            this.network = NetworkCode.decode(env[2] & 0xFF);
            this.issuer = env[3] & 0xFF;
            this.application = ByteUtils.extractInt(env, 4, 4, false);
            this.issuingDate = ByteUtils.extractInt(env, 8, 2, false);
            this.endDate = ByteUtils.extractInt(env, 10, 2, false);
            this.holderBirthDate = ByteUtils.extractInt(env, 12, 4, false);
            this.holderCompany = env[16] & 0xFF;
            this.holderId = ByteUtils.extractInt(env, 17, 4, false);

            this.profile = Profile.decode(
                    ByteUtils.mostSignificantNibble(env[21]),
                    ByteUtils.mostSignificantNibble(env[23]),
                    ByteUtils.mostSignificantNibble(env[26])
            );

            this.prof1Date = ByteUtils.extractInt(
                    ByteUtils.extractBytes(env, 21 * 8 + 4, 2), 0, 2, false);

            this.prof2Date = ByteUtils.extractInt(env, 24, 2, false);

            this.prof3Date = ByteUtils.extractInt(
                    ByteUtils.extractBytes(env, 26 * 8 + 4, 2), 0, 2, false);

            this.holderPadding = ByteUtils.leastSignificantNibble(env[28]);
        }

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Event extends File {

        private int id;

        private int version;
        private int transactionNumber;
        private int transactionType;
        private int networkId;
        private int serviceProvider;
        private int locationId;
        private int dateTimeStamp;
        private int amount;
        private int firstServiceProvider;
        private int firstLocationId;
        private int firstDateTimeStamp;
        private int firstPassenger;
        private int firstContractsUsed;
        private int data;

        public Event(int id, byte[] data) {
            super(HexUtil.toHex(data));
            this.id = id;
            Assert.isNull(data, "event frame can not be null");

            if (data.length < RECORD_SIZE)
                throw new IllegalStateException("event frame can not be less than " + RECORD_SIZE);

            this.version              = data[0] & 0xff;
            this.transactionNumber    = ByteUtils.extractInt(data, 1, 3, false);
            this.transactionType      = data[4] & 0xff;
            this.networkId            = data[5] & 0xff;
            this.serviceProvider      = data[6] & 0xff;
            this.locationId           = ByteUtils.extractInt(data, 7, 3, false);
            this.dateTimeStamp        = ByteUtils.extractInt(data, 10, 4, false);
            this.amount               = ByteUtils.extractInt(data, 14, 3, false);
            this.firstServiceProvider = data[17] & 0xff;
            this.firstLocationId      = ByteUtils.extractInt(data, 18, 3, false);
            this.firstDateTimeStamp   = ByteUtils.extractInt(data, 21, 4, false);
            this.firstPassenger       = data[25] & 0xff;
            this.firstContractsUsed   = data[26] & 0xff;
            this.data                 = ByteUtils.extractInt(data, 27, 2, false);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Contract extends File {

        private int id;

        private int version;
        private ContractStatus status;
        private int rfu;
        private int startDate;
        private int duration;
        private int network;
        private Provider provider;
        private Modality modality;
        private int counterCode;
        private Tariff tariff;
        private int journeyInterChanges;
        private int vehicleClassAllowed;
        private int restrictTime;
        private int restrictCode;
        private int periodJourney;
        private long location;
        private int saleDate;
        private int saleSam;
        private int authKvc;
        private int authenticator;

        public Contract(int id, byte[] data) {
            super(HexUtil.toHex(data));
            this.id = id;

            if (data == null)
                throw new IllegalArgumentException("Null data.");

            if (data.length > RECORD_SIZE)
                throw new IllegalArgumentException("Data overflow.");

            if (data.length < RECORD_SIZE) {
                byte[] tmp = new byte[RECORD_SIZE];
                System.arraycopy(data, 0, tmp, 0, data.length);
                data = tmp;
            }

            this.version              = data[0] & 0xff;
            this.status               = ContractStatus.decode(data[1] & 0xff);
            this.rfu                  = (data[2] & 0b11000000) >> 6;
            this.startDate            = ByteUtils.extractInt(data, 2, 2, false) & 0x3fff;
            this.duration             = data[4] & 0xff;
            this.network              = data[5] & 0xff;
            this.provider             = Provider.decode(data[6] & 0xff);

            this.modality             = Modality.decode((data[7] & 0b10000000) >> 7);
            this.counterCode          = (data[7] & 0b01100000) >> 5;
            this.tariff               = Tariff.decode((data[7] & 0b00011111));

            this.journeyInterChanges  = (data[8] & 0b10000000) >> 7;
            this.vehicleClassAllowed  = (data[8] & 0b01100000) >> 5;
            this.restrictTime         = (data[8] & 0b00011111);

            this.restrictCode         = data[9] & 0xff;
            this.periodJourney        = data[10] & 0xff;
            this.location             = ByteUtils.extractLong(data, 11, 5, false);
            this.saleDate             = ByteUtils.extractInt(data, 16, 2, false);
            this.saleSam              = ByteUtils.extractInt(data, 18, 4, false);
            this.authKvc              = data[25] & 0xff;
            this.authenticator        = ByteUtils.extractInt(data, 26, 3, false);
        }
    }

    public static class Contracts implements List<Contract> {

        private final List<Contract> contracts = new ArrayList<>();

        /**
         * Finds all contracts that match the given condition.
         *
         * @param condition predicate to test contracts
         * @return list of matching contracts
         */
        public List<Contract> find(Predicate<Contract> condition) {
            return contracts.stream()
                    .filter(condition)
                    .collect(Collectors.toList());
        }

        /**
         * Finds the first contract that matches the given condition.
         *
         * @param condition predicate to test contracts
         * @return optional containing the first matching contract, or empty if none
         */
        public Optional<Contract> findFirst(Predicate<Contract> condition) {
            return contracts.stream()
                    .filter(condition)
                    .findFirst();
        }

        /**
         * Checks if the first contract in the list matches the given condition.
         *
         * @param condition predicate to test the first contract
         * @return true if the first contract matches, false otherwise
         */
        public boolean isFirst(Predicate<Contract> condition) {
            if (contracts.isEmpty()) return false;
            return condition.test(contracts.get(0));
        }

        /**
         * Checks if the last contract in the list matches the given condition.
         *
         * @param condition predicate to test the last contract
         * @return true if the last contract matches, false otherwise
         */
        public boolean isLast(Predicate<Contract> condition) {
            if (contracts.isEmpty()) return false;
            return condition.test(contracts.get(contracts.size() - 1));
        }

        /**
         * Returns the indices of all contracts that match the given condition.
         *
         * @param condition predicate to test contracts
         * @return list of indices of matching contracts
         */
        public List<Integer> findIndices(Predicate<Contract> condition) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < contracts.size(); i++) {
                if (condition.test(contracts.get(i))) {
                    indices.add(i);
                }
            }
            return indices;
        }

        @Override
        public int size() {
            return contracts.size();
        }

        @Override
        public boolean isEmpty() {
            return contracts.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return contracts.contains(o);
        }

        @Override
        public Iterator<Contract> iterator() {
            return contracts.iterator();
        }

        @Override
        public Object[] toArray() {
            return contracts.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return contracts.toArray(a);
        }

        @Override
        public boolean add(Contract contract) {
            return contracts.add(contract);
        }

        @Override
        public boolean remove(Object o) {
            return contracts.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return contracts.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Contract> c) {
            return contracts.addAll(c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends Contract> c) {
            return contracts.addAll(index, c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return contracts.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return contracts.retainAll(c);
        }

        @Override
        public void clear() {
            contracts.clear();
        }

        @Override
        public boolean equals(Object o) {
            return contracts.equals(o);
        }

        @Override
        public int hashCode() {
            return contracts.hashCode();
        }

        @Override
        public Contract get(int index) {
            return contracts.get(index);
        }

        @Override
        public Contract set(int index, Contract element) {
            return contracts.set(index, element);
        }

        @Override
        public void add(int index, Contract element) {
            contracts.add(index, element);
        }

        @Override
        public Contract remove(int index) {
            return contracts.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return contracts.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return contracts.lastIndexOf(o);
        }

        @Override
        public ListIterator<Contract> listIterator() {
            return contracts.listIterator();
        }

        @Override
        public ListIterator<Contract> listIterator(int index) {
            return contracts.listIterator(index);
        }

        @Override
        public List<Contract> subList(int fromIndex, int toIndex) {
            return contracts.subList(fromIndex, toIndex);
        }
    }

}
