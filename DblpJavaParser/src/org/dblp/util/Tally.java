package org.dblp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A class for counting the frequency of different items. Objects a and b are considered the same
 * observed item for this tally iff <tt>a.equals(b)</tt>.
 *
 * @author Marcel R. Ackermann
 * @version 2013-02-14
 * @param <K> The class of objects to be tallied.
 */
public class Tally<K> {

    /** A wrapper class representing a single tally entry. */
    private class Entry {

        /** The tally item. */
        public final K item;
        /** The frequency of the tally item. */
        public int freq;
        /** The order rank of the tally item. */
        public int rank;

        /**
         * Constructs a new tally entry.
         *
         * @param item The item.
         * @param freq The initial number of observations of the item.
         * @param rank The initial rank of the item.
         */
        public Entry(K item, int freq, int rank) {
            this.item = item;
            this.freq = freq;
            this.rank = rank;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "[" + this.rank + ": " + this.freq + "x " + this.item + "]";
        }
    }

    /** The random access to tally entries. */
    private Map<K, Entry> data = null;

    /** The ordered access to tally entries. */
    private List<Entry> order = null;

    /** The number of distinct observations in the tally, i.e., the total of all frequencies. */
    private int nobs = 0;

    /**
     * Creates a new tally of the given size.
     * <p>
     * The initial <tt>size</tt> is the number of different observed items a new tally reserves. If
     * the number of actual observed items exceeds the initial size, it is adjusted at runtime. A
     * good estimate of the actual number of observed items helps to reduce the running time while
     * not wasting memory.
     *
     * @param size The initial size of the tally.
     * @throws IllegalArgumentException if <tt>size</tt> is non-positive.
     */
    public Tally(int size) {

        if (size < 1) {
            String errmsg = "Initial size must be positive.";
            throw new IllegalArgumentException(errmsg);
        }
        int capacity = 4 * size / 3 + 1;
        this.data = new HashMap<K, Entry>(capacity, 0.75f);
        this.order = new ArrayList<Entry>(size);
        this.nobs = 0;
    }

    /**
     * Creates a new tally with the default initial size of 12 distinct items.
     * <p>
     * If the number actual observed items exceeds the initial size, it is adjusted at runtime. A
     * good estimate of the actual number of observed items helps to reduce the running time while
     * not wasting memory. To specify the initial capacity explicitly, use
     * {@link org.dblp.util.Tally#Tally(int)}.
     */
    public Tally() {

        this(12);
    }

    /**
     * Adds new observations to the tally.
     *
     * @param item The observation.
     * @param frequency The frequency of occurrence of the item.
     * @throws IllegalArgumentException if frequency is negative.
     */
    public void add(K item, int frequency) throws IllegalArgumentException {

        if (frequency == 0) return;
        if (frequency < 1) {
            String errmsg = "Frequency must not be negative.";
            throw new IllegalArgumentException(errmsg);
        }
        // retrieve entry
        Entry entry;
        if (!this.data.containsKey(item)) {
            entry = new Entry(item, 0, this.order.size());
            this.data.put(item, entry);
            this.order.add(entry);
        }
        else {
            entry = this.data.get(item);
        }
        // set frequency value
        entry.freq += frequency;

        // bubble sort entry to maintain ordering
        Entry otherEntry = null;
        if (entry.rank > 0) {
            otherEntry = this.order.get(entry.rank - 1);
        }
        while (entry.rank > 0 && entry.freq > otherEntry.freq) {
            this.order.set(entry.rank, otherEntry);
            otherEntry.rank = entry.rank;
            entry.rank = entry.rank - 1;
            if (entry.rank > 0) {
                otherEntry = this.order.get(entry.rank - 1);
            }
        }
        this.order.set(entry.rank, entry);

        // update total number of observations
        this.nobs += frequency;
    }

    /**
     * Adds one new observation to the tally.
     *
     * @param item The observation.
     */
    public void add(K item) {

        this.add(item, 1);
    }

    /**
     * Removes all observations from this tally. This tally will be empty after the call returns.
     */
    public void clear() {
        this.data.clear();
        this.order.clear();
        this.nobs = 0;
    }

    /**
     * Checks whether there is at least one observation of that item in the tally.
     *
     * @param item The item.
     * @return True if there has been at least one observation of that item, otherwise false.
     */
    public boolean has(K item) {

        return (this.data.containsKey(item));
    }

    /**
     * Get the number of observations of that item.
     *
     * @param item The item.
     * @return The number of observations.
     */
    public int get(K item) {

        if (this.has(item)) return (this.data.get(item).freq);
        else return (0);
    }

    /**
     * Returns a Set view of the items observed in this tally with at least the given number of
     * observations.
     *
     * @param number The minimum number of observations.
     * @return The observed items.
     */
    public Set<K> items(int number) {

        if (number <= 0) return (this.data.keySet());

        Set<K> result = new HashSet<K>();
        for (Entry entry : this.order) {
            if (entry.freq < number) break;
            result.add(entry.item);
        }
        return (result);
    }

    /**
     * Returns a Set view of all the items observed in this tally.
     *
     * @return The observed items.
     */
    public Set<K> items() {

        return (this.data.keySet());
    }

    /**
     * Returns a List view of the most frequent items in this tally, ordered by descending
     * frequency.
     *
     * @param number The number of items to be returned. If this exceeds the number of items in this
     *            tally, then all items are returned.
     * @return The most frequent observed items.
     */
    public List<K> getTop(int number) {

        if (number > this.order.size()) number = this.order.size();

        List<K> result = new ArrayList<K>(number);
        for (int i = 0; i < number; i++) {
            result.add(this.order.get(i).item);
        }
        return (result);
    }

    /**
     * Returns a List view of all items in this tally, ordered by descending frequency.
     *
     * @return The all observed items, ordered by descending frequency.
     */
    public List<K> getTop() {

        return (this.getTop(this.order.size()));
    }

    /**
     * Returns the number of items observed in this tally.
     *
     * @return The number of observed items.
     */
    public int numberOfItems() {

        return (this.data.size());
    }

    /**
     * Returns the number of observations in this tally.
     *
     * @return The number of individual observations.
     */
    public int numberOfObservations() {

        return (this.nobs);
    }

    // // TODO: testing
    // @SuppressWarnings("javadoc")
    // public static void main(String[] args) throws FileNotFoundException, IOException {
    //
    // Tally<String> tally = new Tally<String>();
    //
    // String words = FileUtil.readFile(args[0]);
    // for (String word : words.split("\\s+")) {
    // tally.add(word.toLowerCase());
    // }
    //
    // for (String word : tally.getTop(10)) {
    // System.out.println(tally.get(word) + "x " + word);
    // }
    // System.out.println();
    // System.out.println(tally.numberOfItems() + " items");
    // System.out.println(tally.numberOfObservations() + " obs");
    // }

}
