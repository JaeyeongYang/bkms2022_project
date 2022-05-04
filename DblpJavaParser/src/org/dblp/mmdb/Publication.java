package org.dblp.mmdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;


/**
 * A class representing dblp publication records.
 */
public class Publication extends Record {

    /** The stream venue of this publication. */
    protected PublicationStreamTitle stream;
    /** The toc containing this publication. */
    protected TableOfContents toc;
    /** Integer representation of this records year entry. */
    protected int year;

    /**
     * Create a new, empty Publication container. This constructor is intended only for extensions,
     * and the implementing class has to take care of initializing all member fields properly.
     */
    protected Publication() {}

    /**
     * Create a new Publication. This constructor is intended for use in the MMDB only.
     *
     * @param dblp The MMDB.
     * @param key The record key.
     * @param mdate The record mdate.
     * @param xml The dblp record as byte array, or <code>null</code> if an empty default template
     *            should be used.
     * @param names The PersonNames referenced in this record.
     * @param toc The table of contents referenced in this record.
     * @param stream The publication stream venue referenced in this record.
     * @param year The publishing year.
     */
    Publication(MmdbInterface dblp, String key, int mdate, byte[] xml, PersonName[] names,
            TableOfContents toc, PublicationStreamTitle stream, int year) {
        // protected Publication(String key, int mdate, byte[] xml, PersonName[] names,
        // PublicationStreamTitle stream, int year) {
        this.dblp = dblp;
        this.key = key;
        this.mdate = mdate;
        this.xml = xml;
        this.names = names;
        this.toc = toc;
        this.stream = stream;
        this.year = year;
    }

    /**
     * Retrieves all external ID types stored in this publication record. The internal type
     * {@link PublicationIDType#DBLP} will be ignored by this method.
     *
     * @return The types.
     */
    public Set<PublicationIDType> getIdTypes() {
        Set<PublicationIDType> result = new HashSet<>(2);
        for (Field eeField : this.getFields("ee")) {
            PublicationIDType type = PublicationIDType.of(eeField.value());
            if (type != null && type != PublicationIDType.DBLP) result.add(type);
        }
        return result;
    }

    /**
     * Retrieves all external IDs stored in this publication record matching the given type.
     * Querying for {@link PublicationIDType#DBLP} will just return a single-item list with the same
     * ID result as {@link #getKey()}.
     *
     * @param type The ID type.
     * @return The IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public List<String> getIds(PublicationIDType type) {
        if (type == null) throw new NullPointerException();
        if (type == PublicationIDType.DBLP) return Arrays.asList(this.getKey());

        List<String> result = new ArrayList<>(2);
        for (Field eeField : this.getFields("ee")) {
            PublicationIDType eeType = PublicationIDType.of(eeField.value());
            if (eeType == type) result.add(eeType.getID(eeField.value()));
        }
        return result;
    }

    /**
     * Retrieves a sequential stream of all external ID types stored in this publication record. The
     * internal type {@link PublicationIDType#DBLP} will be ignored by this method.
     *
     * @return The stream of types.
     */
    public Stream<PublicationIDType> idTypes() {
        return this.fields("ee").map(field -> field.value()).map(PublicationIDType::of).filter(Objects::nonNull).filter(type -> !type.equals(PublicationIDType.DBLP));
    }

    /**
     * Retrieves a sequential stream of all external IDs stored in this publication record matching
     * the given type. Querying for {@link PublicationIDType#DBLP} will just return a single-item
     * stream with the same ID result as {@link #getKey()}.
     *
     * @param type The ID type.
     * @return The stream of IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public Stream<String> ids(PublicationIDType type) {
        if (type == null) throw new NullPointerException();
        if (type == PublicationIDType.DBLP) return Stream.of(this.getKey());

        return this.fields("ee").filter(field -> type.equals(PublicationIDType.of(field.value()))).map(field -> PublicationIDType.of(field.value()).getID(field.value()));
    }

    /**
     * Get the table of contents object of this record.
     *
     * @return The toc.
     */
    public TableOfContents getToc() {
        return this.toc;
    }

    /**
     * Get the publication stream venue of this record.
     *
     * @return The stream venue.
     */
    public PublicationStreamTitle getPublicationStream() {
        return this.stream;
    }

    /**
     * Get the publication stream venue of this record as BookTitle object.
     *
     * @return The booktitle.
     */
    public BookTitle getBooktitle() {
        PublicationStreamTitle pst = getPublicationStream();
        if (pst == null) return null;
        if (pst instanceof BookTitle) return (BookTitle) pst;
        if (pst instanceof MultiStreamTitle) { return ((MultiStreamTitle) pst).getBookTitle(); }
        return null;
    }

    /**
     * Get the publication stream venue of this record as Jornal object.
     *
     * @return The journal.
     */
    public JournalTitle getJournal() {
        PublicationStreamTitle pst = getPublicationStream();
        if (pst == null) return null;
        if (pst instanceof JournalTitle) return (JournalTitle) pst;
        if (pst instanceof MultiStreamTitle) { return ((MultiStreamTitle) pst).getJournalTitle(); }
        return null;
    }

    /**
     * Get the publishing year of this record.
     *
     * @return The year.
     */
    public int getYear() {
        return this.year;
    }

    @Override
    public String getXml() {
        if (this.xml == null) return null;
        final StringBuilder sb = xmlBuild();
        int pos;
        if ((pos = sb.indexOf("<2")) >= 0)
            fillPlaceholderTag(sb, pos, "journal", getJournal().getTitle());
        if ((pos = sb.indexOf("<3")) >= 0)
            fillPlaceholderTag(sb, pos, "booktitle", getBooktitle().getTitle());
        if ((pos = sb.indexOf("<4")) >= 0)
            fillPlaceholderTag(sb, pos, "year", Integer.toString(this.year));
        return sb.toString();
    }
}
