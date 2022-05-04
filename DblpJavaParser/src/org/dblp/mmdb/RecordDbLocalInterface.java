package org.dblp.mmdb;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;


/**
 * The local query interface for accessing dblp records from a dblp record DB structure.
 */
public interface RecordDbLocalInterface {

    /**
     * Retrieve the record with the given key.
     *
     * @param key The key of the record to be retrieved.
     * @return The record, or {@code null} if there is no such record.
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
    default public boolean hasPublication(String key) throws NullPointerException {
        return this.getPublication(key) != null;
    }

    /**
     * Search the publication with the given record key.
     *
     * @param key The key of the publication to be retrieved.
     * @return The publication, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public Publication getPublication(String key) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of the publications of a given person in dblp.
     *
     * @param pers The person.
     * @return The publications.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    default public Collection<Publication> getPublications(Person pers)
            throws NullPointerException {
        return pers.getPublications();
    }

    /**
     * Retrieves a sequential stream of the publications of a given person in dblp.
     *
     * @param pers The person.
     * @return The stream of publications.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    default public Stream<Publication> publications(Person pers) throws NullPointerException {
        return getPublications(pers).stream();
    }

    /**
     * Retrieve the number of publications of a given person in dblp.
     *
     * @param pers The person.
     * @return The number of publications of that person.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public default int numberOfPublications(Person pers) throws NullPointerException {
        return pers.numberOfPublications();
    }

    /**
     * Retrieves an unmodifiable Collection view of the publications of a given person in dblp.
     * Publications of any alias name of the given name are retrieved, too.
     *
     * @param name The person.
     * @return The publications. If the given person name is not in dblp, then this method will
     *         return an empty collection.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    default public Collection<Publication> getPublications(PersonName name)
            throws NullPointerException {
        Person pers = name.getPerson();
        if (pers == null) return Collections.emptyList();
        return getPublications(pers);
    }

    /**
     * Retrieves a sequential stream of the publications of a given person in dblp. Publications of
     * any alias name of the given name are retrieved, too.
     *
     * @param name The person.
     * @return The stream of publications. If the given person name is not in dblp, then this method
     *         will return an empty stream.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    default public Stream<Publication> publications(PersonName name) throws NullPointerException {
        return publications(name.getPerson());
    }

    /**
     * Retrieve the number of publications of a given person. Publications of any alias name of the
     * given name are counted, too.
     *
     * @param name The person.
     * @return The number of publications of that person. If the given person name is not in dblp,
     *         then this method will return {@code 0}.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default int numberOfPublications(PersonName name) throws NullPointerException {
        Person pers = name.getPerson();
        if (pers == null) return 0;
        return numberOfPublications(pers);
    }

    /**
     * Retrieves an unmodifiable Collection view of the publications of a given table of contents in
     * dblp.
     *
     * @param toc The toc.
     * @return The publications.
     * @throws NullPointerException if the specified toc is {@code null}.
     */
    default public Collection<Publication> getPublications(TableOfContents toc)
            throws NullPointerException {
        return toc.getPublications();
    }

    /**
     * Retrieves a sequential stream of the publications of a given table of contents in dblp.
     *
     * @param toc The toc.
     * @return The stream of publications.
     * @throws NullPointerException if the specified toc is {@code null}.
     */
    default public Stream<Publication> publications(TableOfContents toc)
            throws NullPointerException {
        return getPublications(toc).stream();
    }

    /**
     * Retrieve the number of publications of a given table of contents in dblp.
     *
     * @param toc The toc.
     * @return The number of publications of that toc.
     * @throws NullPointerException if the specified toc is {@code null}.
     */
    public default int numberOfPublications(TableOfContents toc) throws NullPointerException {
        return toc.size();
    }

    // ------------------------------------------------------------
    // Persons
    // ------------------------------------------------------------

    /**
     * Checks whether a person with the given record key is present in dblp.
     *
     * @param key The key of the person to be checked.
     * @return {@code true} if the person with the specified key is present, otherwise
     *         {@code false}.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    default public boolean hasPerson(String key) throws NullPointerException {
        return this.getPerson(key) != null;
    }

    /**
     * Retrieve the Person with the given record key. Person redirections ("crossref") are resolved
     * silently.
     *
     * @param key The key of the Person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public Person getPerson(String key) throws NullPointerException;

    /**
     * Checks whether a person with the given dblp PID is present in dblp.
     *
     * @param pid The PID of the person to be checked.
     * @return {@code true} if the person with the specified PID is present, otherwise
     *         {@code false}.
     * @throws NullPointerException if the specified PID is {@code null}.
     */
    default public boolean hasPersonByPid(String pid) throws NullPointerException {
        return this.getPersonByPid(pid) != null;
    }

    /**
     * Retrieve the person with the given external ID.
     *
     * @param pid The PID of the person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified PID is {@code null}.
     */
    default public Person getPersonByPid(String pid) throws NullPointerException {
        return this.getPerson("homepages/" + pid);
    }

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
     * Checks whether a person with the exact given dblp person name is present in dblp. The person
     * name must include any potential homonym number.
     *
     * @param name The name of the person to be checked.
     * @return {@code true} if the person with the specified key is present, otherwise {@code false}
     *         .
     * @throws NullPointerException if the specified name is {@code null}.
     */
    default public boolean hasPersonByName(String name) throws NullPointerException {
        return getPersonByName(name) != null;
    }

    /**
     * Retrieve the Person with the given name.
     *
     * @param name The name of the Person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Person getPerson(PersonName name) throws NullPointerException {
        return getPersonByName(name.name());
    }

    /**
     * Retrieve the Person with the exact given dblp person name. The name string must include any
     * potential homonym number.
     *
     * @param name The name of the Person to be retrieved.
     * @return Person object, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Person getPersonByName(String name) throws NullPointerException {
        PersonName nameObj = getPersonName(name);
        if (nameObj == null) return null;
        return nameObj.getPerson();
    }

    /**
     * Retrieve the person name object with the exact given dblp person name if it exists in dblp.
     * The name string must include any potential homonym number.
     *
     * @param name The name of the Person to be retrieved.
     * @return The person name object, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public PersonName getPersonName(String name) throws NullPointerException;

    // ------------------------------------------------------------
    // Homonyms
    // ------------------------------------------------------------

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
    public default Stream<Person> coauthors(Person person) throws NullPointerException {
        return this.getCoauthors(person).stream();
    }

    /**
     * Retrieve the number of coauthors of the given person.
     *
     * @param person The person.
     * @return The number of coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public default int numberOfCoauthors(Person person) throws NullPointerException {
        return this.getCoauthors(person).size();
    }

    /**
     * Return the number of coauthor communities of the given person. Disambiguation profiles are
     * considered to be valid coauthor nodes that link the network.
     *
     * @param person The person.
     * @return The number of communities.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public int numberOfCoauthorCommunities(Person person) throws NullPointerException;

    /**
     * Retrieve a specific coauthor community of a given person. Disambiguation profiles are
     * considered to be valid coauthor nodes that link the network.
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
     * Retrieve the community index of a coauthor of a given person. Disambiguation profiles are
     * considered to be valid coauthor nodes that link the network.
     *
     * @param person The central person.
     * @param coauthor The coauthor to look up in the coauthor communities of {@code person}.
     * @return The community index between {@code 0} and {@code getNumberOfCommunities())}, or
     *         {@code -1} if no such coauthor exists.
     * @throws NullPointerException if one of the specified persons is {@code null}.
     */
    public int getCoauthorCommunityIndex(Person person, Person coauthor)
            throws NullPointerException;

    /**
     * Checks whether the two given persons are coauthors in dblp.
     *
     * @param first The first person.
     * @param second The second person.
     * @return {@code true} if both persons are coauthors, otherwise {@code false}.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    public boolean hasCoauthors(Person first, Person second) throws NullPointerException;

    /**
     * Retrieves the number of coauthored publications of the two given persons in dblp.
     *
     * @param first The first person.
     * @param second The second person.
     * @return The number of coauthored publications.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    public int numberOfCoPublications(Person first, Person second) throws NullPointerException;

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
    default public boolean hasToc(String key) throws NullPointerException {
        return getToc(key) != null;
    }

    /**
     * Retrieve the table of contents with the given toc key.
     *
     * @param key The toc key.
     * @return The toc object, or {@code null} if there is no such toc.
     * @throws NullPointerException if the specified key is {@code null}.
     */
    public TableOfContents getToc(String key) throws NullPointerException;

}
