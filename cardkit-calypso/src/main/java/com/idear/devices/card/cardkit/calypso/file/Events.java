package com.idear.devices.card.cardkit.calypso.file;

import com.idear.devices.card.cardkit.core.datamodel.calypso.Calypso;
import com.idear.devices.card.cardkit.core.io.card.file.File;
import com.idear.devices.card.cardkit.core.utils.ByteBuilder;

import java.util.*;

public class Events extends File<Events> implements List<Event> {

    private final List<Event> events = new ArrayList<>();

    public Events() {
        super(null, Calypso.EVENT_FILE);
    }

    /**
     * Adds a new event to the list and removes the first one if the list is not empty.
     * Useful for keeping a fixed-size rolling list of events.
     */
    public void append(Event event) {
        if (!events.isEmpty()) {
            Event min = Collections.min(events, Comparator.comparingInt(Event::getTransactionNumber));
            events.remove(min);
        }

        events.add(event);

        events.sort(Comparator.comparingInt(Event::getTransactionNumber).reversed());

        int id = 1;
        for (Event e : events) {
            e.setId(id++);
        }
    }

    public Event getLast() {
        Event event = null;

        for(Event e : events) {
            if (event == null || event.getTransactionNumber() < e.getTransactionNumber())
                event = e;
        }

        return event;
    }

    public int getNextTransactionNumber() {
        return getLast().getTransactionNumber() + 1;
    }

    public byte[] unparse() {
        ByteBuilder byteBuilder = new ByteBuilder();
        events.sort(Comparator.comparingInt(Event::getId));

        for (Event event : events) {
            byteBuilder.append(event.unparse());
        }

        return byteBuilder.toByteArray();
    }

    @Override
    public Events parse(byte[] data) {
        return null;
    }

    @Override
    public int size() {
        return events.size();
    }

    @Override
    public boolean isEmpty() {
        return events.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return events.contains(o);
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }

    @Override
    public Object[] toArray() {
        return events.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return events.toArray(a);
    }

    @Override
    public boolean add(Event event) {
        return events.add(event);
    }

    @Override
    public boolean remove(Object o) {
        return events.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return events.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Event> c) {
        return events.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Event> c) {
        return events.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return events.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return events.retainAll(c);
    }

    @Override
    public void clear() {
        events.clear();
    }

    @Override
    public Event get(int index) {
        return events.get(index);
    }

    @Override
    public Event set(int index, Event element) {
        return events.set(index, element);
    }

    @Override
    public void add(int index, Event element) {
        events.add(index, element);
    }

    @Override
    public Event remove(int index) {
        return events.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return events.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return events.lastIndexOf(o);
    }

    @Override
    public ListIterator<Event> listIterator() {
        return events.listIterator();
    }

    @Override
    public ListIterator<Event> listIterator(int index) {
        return events.listIterator(index);
    }

    @Override
    public List<Event> subList(int fromIndex, int toIndex) {
        return events.subList(fromIndex, toIndex);
    }
}
