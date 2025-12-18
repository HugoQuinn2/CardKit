package com.idear.devices.card.cardkit.core.datamodel.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.calypso.constant.ContractStatus;
import com.idear.devices.card.cardkit.core.exception.CardException;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Contracts implements List<Contract> {

    private final List<Contract> contracts = new ArrayList<>();

    /**
     * Find first contract that status is accepted {@link ContractStatus#isAccepted()}
     *
     * @return first contract that status is accepted {@link ContractStatus#isAccepted()}
     * @throws CardException if the card have any valid contract
     */
    public Contract getFirstContractValid() {
        return this.findFirst(c -> c.getStatus().decode(ContractStatus.RFU).isAccepted())
                .orElseThrow(() -> new CardException("card without valid contract"));
    }

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