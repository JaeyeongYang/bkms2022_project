package org.dblp.mmdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


/**
 * An class representing a logical toc unit in dblp.
 */
public class TableOfContents {

    /** The key of this toc. */
    protected String key;
    /** The list of publication in this toc. */
    protected List<Publication> publs;

    /**
     * Create a new, empty TableOfContents container. This constructor is intended only for
     * extensions, and the implementing class has to take care of initializing all member fields
     * properly.
     */
    protected TableOfContents() {}

    /**
     * Create a new table of contents.
     *
     * @param key The toc key .
     */
    public TableOfContents(String key) {
        this.key = key;
        this.publs = new ArrayList<>();
    }

    /**
     * Adds the given publication to this toc.
     *
     * @param publ The publication.
     */
    void add(Publication publ) {
        this.publs.add(publ);
    }

    /**
     * Adds the given publications to this toc.
     *
     * @param publications The publications.
     */
    void addAll(Collection<Publication> publications) {
        this.publs.addAll(publications);
    }

    /**
     * Retrieve the BHT key of this toc.
     *
     * @return The key.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Retrieve the relative page URL of this toc.
     *
     * @return relative URL.
     */
    public String getPageUrl() {
        String[] parts = this.key.split("\\.", 2);
        return parts[0];
    }

    /**
     * Retrieve an unmodifiable Collection view of all publications in this toc.
     *
     * @return The publications.
     */
    public Collection<Publication> getPublications() {
        return Collections.unmodifiableList(this.publs);
    }

    /**
     * Retrieve a sequential stream of all publications in this toc.
     *
     * @return The stream of publications.
     */
    public Stream<Publication> publications() {
        return this.publs.stream();
    }

    /**
     * Retrieve the number of publications in this toc.
     *
     * @return The number of publications.
     */
    public int size() {
        return this.publs.size();
    }

}
