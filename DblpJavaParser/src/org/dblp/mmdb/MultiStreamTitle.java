package org.dblp.mmdb;

import java.util.ArrayList;
import java.util.List;


/**
 * A class representing the error case of ambiguous or even contradicting parent stream information
 * within a {@link Publication}. This class is mainly intended to provide backward comparability
 * with erroneous, historical dblp records.
 * <p>
 * Objects of this class build upon an already existing (primitive) stream title which serves as the
 * primary title for most purposes. Further stream titles can be added using
 * {@link #add(PublicationStreamTitle)}. The first encountered objects of each subclasses
 * {@link BookTitle}, {@link JournalTitle}, etc. will serve as primary title for methods
 * {@link #getBookTitle()}, {@link #getJournalTitle()}, etc., respectively.
 */
public class MultiStreamTitle extends PublicationStreamTitle {

    /** The primary encountered publication stream title. */
    private final PublicationStreamTitle primary;
    /** The encountered book titles in order of appearance. */
    private final List<BookTitle> bookTitles;
    /** The encountered journal titles in order of appearance. */
    private final List<JournalTitle> journalTitles;

    /**
     * Create a new MultiStreamTitle.
     *
     * @param primary The first and primary stream title.
     */
    protected MultiStreamTitle(PublicationStreamTitle primary) {
        super(primary.title);
        this.primary = primary;
        this.bookTitles = new ArrayList<>(1);
        this.journalTitles = new ArrayList<>(1);
        add(primary);
    }

    /**
     * Add a newly encountered stream title.
     *
     * @param stream The next stream title.
     */
    public void add(PublicationStreamTitle stream) {
        if (stream instanceof BookTitle) this.bookTitles.add((BookTitle) stream);
        else if (stream instanceof JournalTitle) this.journalTitles.add((JournalTitle) stream);
        else throw new UnsupportedOperationException("case " + stream.getClass().getName()
                + " not yet implemented");
    }

    /**
     * Retrieve the primary stream title of this multi stream title.
     *
     * @return The primary title.
     */
    public PublicationStreamTitle getPrimaryTitle() {
        return this.primary;
    }

    /**
     * Checks whether this multi stream title contains a book title.
     *
     * @return {@code true} if this multi stream title contains a book title, otherwise
     *         {@code false}.
     */
    public boolean hasBookTitle() {
        return !this.bookTitles.isEmpty();
    }

    /**
     * Retrieve the primary book title of this multi stream title.
     *
     * @return The title, or {@code null} if no book title exists.
     */
    public BookTitle getBookTitle() {
        if (this.bookTitles.isEmpty()) return null;
        return this.bookTitles.get(0);
    }

    /**
     * Checks whether this multi stream title contains a journal title.
     *
     * @return {@code true} if this multi stream title contains a journal title, otherwise
     *         {@code false}.
     */
    public boolean hasJournalTitle() {
        return !this.journalTitles.isEmpty();
    }

    /**
     * Retrieve the primary journal title of this multi stream title.
     *
     * @return The title, or {@code null} if no journal title exists.
     */
    public JournalTitle getJournalTitle() {
        if (this.journalTitles.isEmpty()) return null;
        return this.journalTitles.get(0);
    }

}
