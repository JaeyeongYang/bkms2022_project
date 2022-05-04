package org.dblp.mmdb;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A general interface for the main memory database (MMDB).
 */
public interface MmdbInterface {

    // ------------------------------------------------------------
    // Publications
    // ------------------------------------------------------------

    /**
     * Retrieve the publication with the given key.
     *
     * @param key The key of the publication to be retrieved.
     * @return The publication, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    Publication getPublication(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all publications in the MMDB.
     *
     * @return The publications.
     */
    Collection<Publication> getPublications();

    /**
     * Retrieves a sequential stream of all publications in the MMDB.
     *
     * @return The stream of publications.
     */
    Stream<Publication> publications();

    /**
     * Retrieve the number of publications in the MMDB.
     *
     * @return The number of publications.
     */
    int numberOfPublications();

    // ------------------------------------------------------------
    // Persons
    // ------------------------------------------------------------

    /**
     * Retrieve the person with the given key. Person redirections ("crossref") will not be
     * resolved.
     *
     * @param key The key of the Person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    Person getPerson(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all persons in the MMDB.
     *
     * @return The persons.
     */
    Collection<Person> getPersons();

    /**
     * Retrieves a sequential stream of all persons in the MMDB.
     *
     * @return The stream of persons.
     */
    Stream<Person> persons();

    /**
     * Retrieve the number of persons in the MMDB.
     *
     * @return The number of persons.
     */
    int numberOfPersons();

    // ------------------------------------------------------------
    // Person redirects
    // ------------------------------------------------------------

    /**
     * Retrieve the redirection record with the given key.
     *
     * @param key The key of the redirection to be retrieved.
     * @return The redirect, or {@code null} if there is no such redirection.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    Redirect getRedirect(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all redirects in the MMDB.
     *
     * @return The redirects.
     */
    Collection<Redirect> getRedirects();

    /**
     * Retrieves a sequential stream of all redirects in the MMDB.
     *
     * @return The stream of redirects.
     */
    Stream<Redirect> redirects();

    /**
     * Retrieve the number of redirects in the MMDB.
     *
     * @return The number of redirects.
     */
    int numberOfRedirects();

    // ------------------------------------------------------------
    // Records
    // ------------------------------------------------------------

    /**
     * Retrieve the record (of any given type, i.e., publication, person, or redirect) with the
     * given key.
     *
     * @param key The key of the record to be retrieved.
     * @return The record, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    default Record getRecord(String key) throws NullPointerException {
        Record rec = getPublication(key);
        if (rec == null) rec = getPerson(key);
        if (rec == null) rec = getRedirect(key);
        return rec;
    }

    /**
     * Retrieves an unmodifiable Collection view of all records (i.e., all publications, persons,
     * and redirects) in the MMDB.
     *
     * @return The records.
     */
    default Collection<Record> getRecords() {
        return records().collect(Collectors.toList());
    }

    /**
     * Retrieves a combined, sequential stream of all records (i.e., all publications, persons, and
     * redirects) in the MMDB.
     *
     * @return The stream of all records.
     */
    default Stream<Record> records() {
        return Stream.concat(Stream.concat(persons(), publications()), redirects());
    }

    /**
     * Retrieve the number of records (of any given type, i.e., publications, persons, and
     * redirects) in the MMDB.
     *
     * @return The number of records.
     */
    default int numberOfRecords() {
        return this.numberOfPublications() + this.numberOfPersons() + this.numberOfRedirects();
    }

    // ------------------------------------------------------------
    // Person names
    // ------------------------------------------------------------

    /**
     * Retrieve the person name object with the given name string.
     *
     * @param name The name string.
     * @return The person name object, or {@code null} if there is no such name.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    PersonName getPersonName(String name) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all person names in the MMDB.
     *
     * @return The person names.
     */
    Collection<PersonName> getPersonNames();

    /**
     * Retrieves a sequential stream of all person names in the MMDB.
     *
     * @return The stream of person names.
     */
    Stream<PersonName> personNames();

    /**
     * Retrieve the number of person names in the MMDB.
     *
     * @return The number of person names.
     */
    int numberOfPersonNames();

    // ------------------------------------------------------------
    // Table of Contents
    // ------------------------------------------------------------

    /**
     * Retrieve the table of contents object with the given toc key.
     *
     * @param key The toc key.
     * @return The toc object, or {@code null} if there is no such toc.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    TableOfContents getToc(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all table of contents in the MMDB.
     *
     * @return The table of contents.
     */
    Collection<TableOfContents> getTocs();

    /**
     * Retrieves a sequential stream of all table of contents in the MMDB.
     *
     * @return The stream of booktitles.
     */
    Stream<TableOfContents> tocs();

    /**
     * Retrieve the number of table of contents in the MMDB.
     *
     * @return The number of tocs.
     */
    int numberOfTocs();

    // ------------------------------------------------------------
    // Booktitles
    // ------------------------------------------------------------

    /**
     * Retrieve the booktitle object with the given booktitle string.
     *
     * @param title The title string.
     * @return The booktitle object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    BookTitle getBookTitle(String title) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all booktitles in the MMDB.
     *
     * @return The booktitles.
     */
    Collection<BookTitle> getBookTitles();

    /**
     * Retrieves a sequential stream of all booktitles in the MMDB.
     *
     * @return The stream of booktitles.
     */
    Stream<BookTitle> bookTitles();

    /**
     * Retrieve the number of booktitles in the MMDB.
     *
     * @return The number of booktitles.
     */
    int numberOfBookTitles();

    // ------------------------------------------------------------
    // Journals
    // ------------------------------------------------------------

    /**
     * Retrieve the journal object with the given journal title string.
     *
     * @param title The title string.
     * @return The journal object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    JournalTitle getJournal(String title) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all journals in the MMDB.
     *
     * @return The journals.
     */
    Collection<JournalTitle> getJournals();

    /**
     * Retrieves a sequential stream of all journals in the MMDB.
     *
     * @return The stream of journals.
     */
    Stream<JournalTitle> journals();

    /**
     * Retrieve the number of journals in the MMDB.
     *
     * @return The number of journals.
     */
    int numberOfJournals();

    // ------------------------------------------------------------
    // Publication stream venues
    // ------------------------------------------------------------

    /**
     * Retrieve the publication stream (of any given type, i.e., booktitle or journal) with the
     * given title string.
     *
     * @param title The title string.
     * @return The publication stream object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    default PublicationStreamTitle getPublicationStream(String title) throws NullPointerException {
        PublicationStreamTitle stream = getBookTitle(title);
        if (stream == null) stream = getJournal(title);
        return stream;
    }

    /**
     * Retrieves an unmodifiable Collection view of all publication stream venues (i.e., all
     * booktitles and journals) in the MMDB.
     *
     * @return The publication stream venues.
     */
    default Collection<PublicationStreamTitle> getPublicationStreams() {
        return Collections.unmodifiableList(publicationStreams().collect(Collectors.toList()));
    }

    /**
     * Retrieves a combined, sequential stream of all publication stream venues (i.e., all
     * booktitles and journals) in the MMDB.
     *
     * @return The stream of all publication stream venues.
     */
    default Stream<PublicationStreamTitle> publicationStreams() {
        return Stream.concat(bookTitles(), journals());
    }

    /**
     * Retrieve the number of publication stream venues (of any given type, i.e., booktitle and
     * journal) in the MMDB.
     *
     * @return The number of publication stream venues.
     */
    default int numberOfPublicationStreams() {
        return this.numberOfBookTitles() + this.numberOfJournals();
    }

}