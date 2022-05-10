package org.dblp;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dblp.mmdb.BookTitle;
import org.dblp.mmdb.JournalTitle;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.PublicationStreamTitle;
import org.dblp.mmdb.Record;
import org.dblp.mmdb.TableOfContents;
import org.dblp.mmdb.datastructure.LocalCoauthorsGraph;


/**
 * The root interface for accessing data from any dblp DB structure.
 */
@Deprecated
public interface DblpInterface {

    /**
     * Search for the record with the given key.
     *
     * @param key The key of the record to be retrieved.
     * @return The record, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public default Record getRecord(String key) throws NullPointerException {
        Record rec = getPublication(key);
        if (rec == null) rec = getPerson(key);
        return rec;
    }

    // ------------------------------------------------------------
    // Publications
    // ------------------------------------------------------------

    /**
     * Checks whether a publication with the given record key is present in dblp.
     *
     * @param key The key of the publication to be checked.
     * @return {@code true} if the publication with the specified key is present, otherwise
     *         {@code false}.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public boolean hasPublication(String key) throws NullPointerException;

    /**
     * Search the publication with the given record key.
     *
     * @param key The key of the publication to be retrieved.
     * @return The publication, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public Publication getPublication(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all publications in dblp.
     *
     * @return The publications.
     */
    public Collection<Publication> getPublications();

    /**
     * Retrieves a sequential stream of all publications in dblp.
     *
     * @return The stream of publications.
     */
    public Stream<Publication> publications();

    /**
     * Retrieve the number of publications in dblp.
     *
     * @return The number of publications.
     */
    public int numberOfPublications();

    /**
     * Retrieves an unmodifiable Collection view of the publications of a given person in dblp.
     *
     * @param pers The person.
     * @return The publications. If {@code pers == null}, then this method will return an empty
     *         collection.
     */
    default public Collection<Publication> getPublications(Person pers) {
        if (pers == null) return Collections.emptyList();
        return pers.getPublications();
    }

    /**
     * Retrieves a sequential stream of the publications of a given person in dblp.
     *
     * @param pers The person.
     * @return The stream of publications. If {@code pers == null}, then this method will return an
     *         empty stream.
     */
    default public Stream<Publication> publications(Person pers) {
        if (pers == null) return Stream.empty();
        return pers.publications();
    }

    /**
     * Retrieve the number of publications of a given person in dblp.
     *
     * @param pers The person.
     * @return The number of publications of that person. If {@code pers == null}, then this method
     *         will return {@code 0}.
     */
    public default int numberOfPublications(Person pers) {
        if (pers == null) return 0;
        return pers.numberOfPublications();
    }

    /**
     * Retrieves an unmodifiable Collection view of the publications of a given person in dblp.
     * Publications of any alias name of the given name are retrieved, too.
     *
     * @param name The person.
     * @return The publications. If the given person name is not in dblp, then this method will
     *         return an empty collection.
     */
    default public Collection<Publication> getPublications(PersonName name) {
        if (name == null) return Collections.emptyList();
        return getPublications(name.getPerson());
    }

    /**
     * Retrieves a sequential stream of the publications of a given person in dblp. Publications of
     * any alias name of the given name are retrieved, too.
     *
     * @param name The person.
     * @return The stream of publications. If the given person name is not in dblp, then this method
     *         will return an empty stream.
     */
    default public Stream<Publication> publications(PersonName name) {
        if (name == null) return Stream.empty();
        return publications(name.getPerson());
    }

    /**
     * Retrieve the number of publications of a given person. Publications of any alias name of the
     * given name are counted, too.
     *
     * @param name The person.
     * @return The number of publications of that person. If the given person name is not in dblp,
     *         then this method will return {@code 0}.
     */
    public default int numberOfPublications(PersonName name) {
        if (name == null) return 0;
        return numberOfPublications(name.getPerson());
    }

    /**
     * Retrieves an unmodifiable Collection view of the publications of a given table of contents in
     * dblp.
     *
     * @param toc The toc.
     * @return The publications. If {@code toc == null}, then this method will return an empty
     *         collection.
     */
    default public Collection<Publication> getPublications(TableOfContents toc) {
        if (toc == null) return Collections.emptyList();
        return toc.getPublications();
    }

    /**
     * Retrieves a sequential stream of the publications of a given table of contents in dblp.
     *
     * @param toc The toc.
     * @return The stream of publications. If {@code toc == null}, this method will return an empty
     *         stream.
     */
    default public Stream<Publication> publications(TableOfContents toc) {
        if (toc == null) return Stream.empty();
        return toc.publications();
    }

    /**
     * Retrieve the number of publications of a given person. Publications of any alias name of the
     * given name are counted, too.
     *
     * @param toc The toc.
     * @return The number of publications of that person. If {@code toc == null}, this method will
     *         return {@code 0}.
     */
    public default int numberOfPublications(TableOfContents toc) {
        if (toc == null) return 0;
        return toc.size();
    }

    // ------------------------------------------------------------
    // Persons
    // ------------------------------------------------------------

    /**
     * Checks whether a person with the given record key is present in dblp.
     *
     * @param key The key of the person to be checked.
     * @return {@code true} if the person with the specified key is present, otherwise {@code false}
     *         .
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public boolean hasPerson(String key) throws NullPointerException;

    /**
     * Search for a Person with a known record key. Person redirections ("crossref") are resolved
     * silently.
     *
     * @param key The key of the Person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public Person getPerson(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all persons in dblp.
     *
     * @return The persons.
     */
    public Collection<Person> getPersons();

    /**
     * Retrieves a sequential stream of all persons in dblp.
     *
     * @return The stream of persons.
     */
    public Stream<Person> persons();

    /**
     * Retrieve the number of persons in dblp.
     *
     * @return The number of persons.
     */
    public int numberOfPersons();

    // ------------------------------------------------------------
    // Person Names
    // ------------------------------------------------------------

    /**
     * Checks whether a person with the given person name is present in dblp.
     *
     * @param name The name of the person to be checked.
     * @return {@code true} if the person with the specified name is present, otherwise
     *         {@code false} .
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default boolean hasPerson(PersonName name) throws NullPointerException {
        return hasPersonByName(name.name());
    }

    /**
     * Search for a Person with a known name.
     *
     * @param name The name of the Person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Person getPerson(PersonName name) throws NullPointerException {
        return getPersonByName(name.name());
    }

    /**
     * Checks whether a person with the exact given dblp person name is present in dblp. The person
     * name must include any potential homonym number.
     *
     * @param name The name of the person to be checked.
     * @return {@code true} if the person with the specified key is present, otherwise {@code false}
     *         .
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public boolean hasPersonByName(String name) throws NullPointerException;

    /**
     * Search for a Person with the exact given dblp person name. The person name must include any
     * potential homonym number.
     *
     * @param name The name of the Person to be retrieved.
     * @return Person object, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public Person getPersonByName(String name) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all person names in dblp.
     *
     * @return The person names.
     */
    public Collection<PersonName> getPersonNames();

    /**
     * Retrieves a sequential stream of all person names in dblp.
     *
     * @return The stream of person names.
     */
    public Stream<PersonName> personNames();

    /**
     * Retrieve the number of person names in dblp.
     *
     * @return The number of person names.
     */
    public int numberOfPersonNames();

    // ------------------------------------------------------------
    // Coauthors
    // ------------------------------------------------------------

    /**
     * Retrieves an unmodifiable list view of all coauthors of the given person.
     *
     * @param person The person.
     * @return The coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public Collection<Person> getCoauthors(Person person) throws NullPointerException;

    /**
     * Retrieves a sequential stream of all coauthors of the given person.
     *
     * @param person The person.
     * @return The stream of coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public Stream<Person> coauthors(Person person) throws NullPointerException;

    /**
     * Retrieve the number of coauthors of the given person.
     *
     * @param person The person.
     * @return The number of coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public int numberOfCoauthors(Person person) throws NullPointerException;

    /**
     * Checks whether the given persons are coauthors in dblp.
     *
     * @param first The first person.
     * @param second The second person.
     * @return {@code true} if both persons are coauthors, otherwise {@code false}.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    public boolean hasCoauthors(Person first, Person second) throws NullPointerException;

    /**
     * Return the number of coauthor communities of the given person.
     *
     * @param person The person.
     * @return The number of communities.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public int numberOfCoauthorCommunities(Person person) throws NullPointerException;

    /**
     * Retrieve a specific coauthor community of a given person.
     *
     * @param person The person.
     * @param index The community index.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     * @throws IndexOutOfBoundsException if the index is out of range
     *             {@code (index < 0 || index >= getNumberOfCommunities())}
     */
    public Collection<Person> getCoauthorCommunity(Person person, int index)
            throws NullPointerException, IndexOutOfBoundsException;

    /**
     * Retrieve the community index of a coauthor of a given person.
     *
     * @param person The central person.
     * @param coauthor The coauthor to look up in the coauthor communities of {@code person}.
     * @return The community index between {@code 0} and {@code getNumberOfCommunities())}, or
     *         {@code -1} if no such coauthor exists.
     * @throws NullPointerException if one of the specified persons is {@code null}.
     */
    public default int getCoauthorCommunityIndex(Person person, Person coauthor)
            throws NullPointerException {
        if (coauthor == null) throw new NullPointerException();
        return getCoauthorGraph(person).getIndex(coauthor);
    }

    /**
     * Retrieve the local coauthor graph of a given person.
     *
     * @param person The person.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public LocalCoauthorsGraph getCoauthorGraph(Person person) throws NullPointerException;

    // ------------------------------------------------------------
    // Table of Contents
    // ------------------------------------------------------------

    /**
     * Checks whether a table of contents with the given toc key is present in dblp.
     *
     * @param key The key of the toc to be checked.
     * @return {@code true} if the toc with the specified key is present, otherwise {@code false} .
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public boolean hasToc(String key) throws NullPointerException;

    /**
     * Search for the table of contents with the given toc key.
     *
     * @param key The toc key.
     * @return The toc object, or {@code null} if there is no such toc.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    TableOfContents getToc(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all table of contents in dblp.
     *
     * @return The table of contents.
     */
    Collection<TableOfContents> getTocs();

    /**
     * Retrieves a sequential stream of all table of contents in dblp.
     *
     * @return The stream of booktitles.
     */
    Stream<TableOfContents> tocs();

    /**
     * Retrieve the number of table of contents in dblp.
     *
     * @return The number of tocs.
     */
    int numberOfTocs();

    // ------------------------------------------------------------
    // Booktitles
    // ------------------------------------------------------------

    /**
     * Search for the booktitle object with the given booktitle string.
     *
     * @param title The title string.
     * @return The booktitle object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    BookTitle getBookTitle(String title) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all booktitles in dblp.
     *
     * @return The booktitles.
     */
    Collection<BookTitle> getBookTitles();

    /**
     * Retrieves a sequential stream of all booktitles in dblp.
     *
     * @return The stream of booktitles.
     */
    Stream<BookTitle> bookTitles();

    // ------------------------------------------------------------
    // Journals
    // ------------------------------------------------------------

    /**
     * Search for the journal object with the given journal title string.
     *
     * @param title The title string.
     * @return The journal object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    JournalTitle getJournal(String title) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all journals in dblp.
     *
     * @return The journals.
     */
    Collection<JournalTitle> getJournals();

    /**
     * Retrieves a sequential stream of all journals in dblp.
     *
     * @return The stream of journals.
     */
    Stream<JournalTitle> journals();

    // ------------------------------------------------------------
    // Publication stream venues
    // ------------------------------------------------------------

    /**
     * Search for the publication stream object with the given title string.
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
     * booktitles and all journals) in dblp.
     *
     * @return The publication stream venues.
     */
    default Collection<PublicationStreamTitle> getPublicationStreams() {
        return publicationStreams().collect(Collectors.toList());
    }

    /**
     * Retrieves a combined, sequential stream of all publication stream venues (i.e., all
     * booktitles and all journals) in dblp.
     *
     * @return The stream of all publication stream venues.
     */
    default Stream<PublicationStreamTitle> publicationStreams() {
        return Stream.concat(bookTitles(), journals());
    }

}
