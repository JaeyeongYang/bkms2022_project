package org.dblp.mmdb;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The global interface for accessing dblp records from a dblp record DB structure.
 */
public interface RecordDbInterface extends RecordDbLocalInterface {

    // ------------------------------------------------------------
    // Ensure lazy building
    // ------------------------------------------------------------

    /**
     * Trigger that the lazy homonymous name map is build, if it has not been build yet.
     */
    public void ensureHomonymMap();

    /**
     * Trigger that the lazy external ID map is build, if it has not been build yet.
     */
    public void ensureIdMap();

    /**
     * Trigger that the lazy coauthor graph data structure is build, if it has not been build yet.
     * <p>
     * <strong>Waring:</strong> Forcing the coauthor graph to build completely is kind of
     * inefficient and requires a lot of main memory.
     */
    public void ensureCoauthorGraph();

    // ------------------------------------------------------------
    // Publications
    // ------------------------------------------------------------

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
     * Checks whether a publication with the given external ID is present in dblp.
     *
     * @param type The ID type to check.
     * @param id The ID of the publication to be checked.
     * @return {@code true} if the publication with the specified ID is present, otherwise
     *         {@code false}.
     * @throws NullPointerException if the specified type or ID is {@code null}.
     */
    default public boolean hasPublication(PublicationIDType type, String id)
            throws NullPointerException {
        return this.getPublication(type, id) != null;
    }

    /**
     * Search the publication with the given external ID.
     *
     * @param type The ID type.
     * @param id The ID of the publication to be retrieved.
     * @return The publication, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified type or ID is {@code null}.
     */
    public Publication getPublication(PublicationIDType type, String id)
            throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of the publications in the MMDB which are decorated
     * with an external ID of the given type.
     *
     * @param type The ID type.
     * @return The publications.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public Collection<Publication> getPublications(PublicationIDType type)
            throws NullPointerException;

    /**
     * Retrieves a sequential stream of the publications in the MMDB which are decorated with an
     * external ID of the given type.
     *
     * @param type The ID type.
     * @return The stream of publications.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public Stream<Publication> publications(PublicationIDType type) throws NullPointerException;

    /**
     * Retrieve the number of publications in the MMDB which are decorated with an external ID of
     * the given type.
     *
     * @param type The ID type.
     * @return The number of publications.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public int numberOfPublications(PublicationIDType type) throws NullPointerException;

    // ------------------------------------------------------------
    // Persons
    // ------------------------------------------------------------

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

    /**
     * Checks whether a person with the given external ID is present in dblp.
     *
     * @param type The ID type.
     * @param id The ID of the person to be checked.
     * @return {@code true} if the person with the specified ID is present, otherwise {@code false}.
     * @throws NullPointerException if the specified type or ID is {@code null}.
     */
    default public boolean hasPerson(PersonIDType type, String id) throws NullPointerException {
        return this.getPerson(type, id) != null;
    }

    /**
     * Retrieve the person with the given external ID.
     *
     * @param type The ID type.
     * @param id The ID of the person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified type or ID is {@code null}.
     */
    public Person getPerson(PersonIDType type, String id) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of the persons in the MMDB which are decorated with
     * an external ID of the given type.
     *
     * @param type The ID type.
     * @return The persons.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public Collection<Person> getPersons(PersonIDType type) throws NullPointerException;

    /**
     * Retrieves a sequential stream of all persons in the MMDB which are decorated with an external
     * ID of the given type.
     *
     * @param type The ID type.
     * @return The stream of persons.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public Stream<Person> persons(PersonIDType type) throws NullPointerException;

    /**
     * Retrieve the number of persons in the MMDB which are decorated with an external ID of the
     * given type.
     *
     * @param type The ID type.
     * @return The number of persons.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public int numberOfPersons(PersonIDType type) throws NullPointerException;

    // ------------------------------------------------------------
    // Person Names
    // ------------------------------------------------------------

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
    // Homonyms
    // ------------------------------------------------------------

    /**
     * Checks whether a given person has any homonyms in dblp other than that given person. Checking
     * of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If the given person has more than one name in dblp, all alias names are checked for homonyms
     * as well.
     *
     * @param pers The person to be checked.
     * @return {@code true} if the person has any homonyms, otherwise {@code false} .
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default boolean hasOtherHomonyms(Person pers) throws NullPointerException {
        return pers.names().anyMatch(name -> hasOtherHomonymsByName(name.name()));
    }

    /**
     * Checks whether the given person name has homonyms in dblp other than that given person name
     * object. Checking of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name of the person to be checked.
     * @return {@code true} if the specified name has any other homonyms, otherwise {@code false} .
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default boolean hasOtherHomonyms(PersonName name) throws NullPointerException {
        return otherHomonyms(name).findAny().isPresent();
    }

    /**
     * Checks whether the given person name has any homonyms in dblp other than that given name
     * string. Checking of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name of the person to be checked.
     * @return {@code true} if the specified name has any other homonyms, otherwise {@code false} .
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default boolean hasOtherHomonymsByName(String name) throws NullPointerException {
        return otherHomonymsByName(name).findAny().isPresent();
    }

    /**
     * Retrieve an unmodifiable collection of all person objects that are homonyms of the given
     * person name object, explicitly <em>excluding</em> that given person name object. Retrieval of
     * homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The homonymous name.
     * @return The homonymous persons, or an empty set if the given name has no other homonyms.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Collection<PersonName> getOtherHomonyms(PersonName name)
            throws NullPointerException {
        return Collections.unmodifiableCollection(otherHomonyms(name).collect(Collectors.toList()));
    }

    /**
     * Retrieve an unmodifiable collection of all person name objects that are homonyms of the given
     * name string, explicitly <em>excluding</em> the person name object of that given name string
     * if it exists in dblp. Retrieval of homonyms is sensitive to case, diacritics, and
     * abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name string.
     * @return The homonymous persons, or an empty set if the given name has no other homonyms.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Collection<PersonName> getOtherHomonymsByName(String name)
            throws NullPointerException {
        return Collections.unmodifiableCollection(otherHomonymsByName(name).collect(Collectors.toList()));
    }

    /**
     * Retrieves a sequential stream of all person objects that are homonyms of the given person
     * name object, explicitly <em>excluding</em> that given person name object if it exists in
     * dblp. Retrieval of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The homonymous name.
     * @return The stream of homonymous persons, or an empty stream if the given name has no other
     *         homonyms.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Stream<PersonName> otherHomonyms(PersonName name) throws NullPointerException {
        return allHomonyms(name).filter(other -> !name.name().equals(other.name()));
    }

    /**
     * Retrieves a sequential stream of all person name objects that are homonyms of the given name
     * string, explicitly <em>excluding</em> the person name object of that given name string if it
     * exists in dblp. Retrieval of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name string.
     * @return The stream of homonymous persons, or an empty stream if the given name has no other
     *         homonyms.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Stream<PersonName> otherHomonymsByName(String name) throws NullPointerException {
        return allHomonymsByName(name).filter(other -> !name.equals(other.name()));
    }

    /**
     * Retrieve the number of homonyms of the given name in dblp, explicitly <em>excluding</em> that
     * given person name object if it exists in dblp. Retrieval of homonyms is sensitive to case,
     * diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The homonymous name.
     * @return The number of homonyms, or {@code 0} if the given name has no other homonyms.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default int numberOfOtherHomonyms(PersonName name) throws NullPointerException {
        return (int) otherHomonyms(name).count();
    }

    /**
     * Retrieve the number of homonyms of the given name in dblp, explicitly <em>excluding</em> the
     * person name object of that given name string if it exists in dblp. Retrieval of homonyms is
     * sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name string.
     * @return The number of homonyms, or {@code 0} if the given name has no other homonyms.
     */
    public default int numberOfOtherHomonymsByName(String name) {
        return (int) otherHomonymsByName(name).count();
    }

    /**
     * Retrieve an unmodifiable collection of all person objects that are homonyms of the given
     * person name object, including that given person name object if it exists in dblp. Retrieval
     * of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The person name.
     * @return The homonymous persons, or an empty set if the given name is not in dblp.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Collection<PersonName> getAllHomonyms(PersonName name)
            throws NullPointerException {
        return getAllHomonymsByName(name.name());
    }

    /**
     * Retrieve an unmodifiable collection of all person name objects that are homonyms of the given
     * name string, including person name objects of that given name string if it exists in dblp.
     * Retrieval of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name string.
     * @return The homonymous persons, or an empty set if the given name is not in dblp.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public Collection<PersonName> getAllHomonymsByName(String name) throws NullPointerException;

    /**
     * Retrieves a sequential stream of all person objects that are homonyms of the given person
     * name object, including that given person name object if it exists in dblp. Retrieval of
     * homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The person name.
     * @return The stream of homonymous persons, or an empty stream if the given name is not in
     *         dblp.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default Stream<PersonName> allHomonyms(PersonName name) throws NullPointerException {
        return allHomonymsByName(name.name());
    }

    /**
     * Retrieves a sequential stream of all person name objects that are homonyms of the given name
     * string, including person name objects of that given name string if it exists in dblp.
     * Retrieval of homonyms is sensitive to case, diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name string.
     * @return The stream of homonymous persons, or an empty stream if the given name is not in
     *         dblp.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public Stream<PersonName> allHomonymsByName(String name) throws NullPointerException;

    /**
     * Retrieve the number of homonyms of the given name in dblp, including that given person name
     * object if it exists in dblp. Retrieval of homonyms is sensitive to case, diacritics, and
     * abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The person name.
     * @return The number of homonyms, or {@code 0} if the given name is not in dblp.
     * @throws NullPointerException if the specified name is {@code null}.
     */
    public default int numberOfAllHomonyms(PersonName name) throws NullPointerException {
        return numberOfAllHomonymsByName(name.name());
    }

    /**
     * Retrieve the number of homonyms of the given name in dblp, including person name objects of
     * that given name string if it exists in dblp. Retrieval of homonyms is sensitive to case,
     * diacritics, and abbreviations.
     * <p>
     * If a person of the exact given name does not exist in dblp, this method may still find
     * homonyms that exist in dblp (e.g., {@code "Wei Wang 9999"} does not exist, but has homonyms
     * {@code "Wei Wang", "Wei Wang 0001"}, and so on).
     *
     * @param name The name string.
     * @return The number of homonyms, or {@code 0} if the given name is not in dblp.
     */
    public int numberOfAllHomonymsByName(String name);

    // ------------------------------------------------------------
    // Coauthors
    // ------------------------------------------------------------

    /**
     * Return the number of coauthor communities of the given person.
     *
     * @param person The person.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor nodes
     *            that link the network. Otherwise, disambiguation profiles are considered to have
     *            no coauthors and end up as singleton cluster nodes in the network.
     * @return The number of communities.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public int numberOfCoauthorCommunities(Person person, boolean allowDisambiguations)
            throws NullPointerException;

    /**
     * Return the number of coauthor communities of the given person. Disambiguation profiles are
     * considered to be valid coauthor nodes that link the network.
     *
     * @param person The person.
     * @return The number of communities.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    @Override
    public default int numberOfCoauthorCommunities(Person person) throws NullPointerException {
        return numberOfCoauthorCommunities(person, true);
    }

    /**
     * Retrieve a specific coauthor community of a given person.
     *
     * @param person The person.
     * @param index The community index, starting with 0.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor nodes
     *            that link the network. Otherwise, disambiguation profiles are considered to have
     *            no coauthors and end up as singleton cluster nodes in the network.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     * @throws IndexOutOfBoundsException if the index is out of range
     *             {@code (index < 0 || index >= getNumberOfCommunities())}
     */
    public Collection<Person> getCoauthorCommunity(Person person, int index,
            boolean allowDisambiguations)
            throws NullPointerException, IndexOutOfBoundsException;

    /**
     * Retrieve a specific coauthor community of a given person. Disambiguation profiles are
     * considered to be valid coauthor nodes that link the network.
     *
     * @param person The person.
     * @param index The community index, starting with 0.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     * @throws IndexOutOfBoundsException if the index is out of range
     *             {@code (index < 0 || index >= getNumberOfCommunities())}
     */
    @Override
    public default Collection<Person> getCoauthorCommunity(Person person, int index)
            throws NullPointerException, IndexOutOfBoundsException {
        return getCoauthorCommunity(person, index, true);
    }

    /**
     * Retrieve the community index of a coauthor of a given person.
     *
     * @param person The central person.
     * @param coauthor The coauthor to look up in the coauthor communities of {@code person}.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor nodes
     *            that link the network. Otherwise, disambiguation profiles are considered to have
     *            no coauthors and end up as singleton cluster nodes in the network.
     * @return The community index between {@code 0} and {@code getNumberOfCommunities())}, or
     *         {@code -1} if no such coauthor exists.
     * @throws NullPointerException if one of the specified persons is {@code null}.
     */
    public default int getCoauthorCommunityIndex(Person person, Person coauthor,
            boolean allowDisambiguations)
            throws NullPointerException {
        if (coauthor == null) throw new NullPointerException();
        return getCoauthorNetwork(person, allowDisambiguations).getIndex(coauthor);
    }

    /**
     * Retrieve the community index of a coauthor of a given person. Disambiguation profiles are
     * considered to be valid coauthor nodes that link the network.
     *
     * @param person The central person.
     * @param coauthor The coauthor to look up in the coauthor communities of {@code person}.
     * @return The community index between {@code 0} and {@code getNumberOfCommunities())-1}, or
     *         {@code -1} if no such coauthor exists.
     * @throws NullPointerException if one of the specified persons is {@code null}.
     */
    @Override
    public default int getCoauthorCommunityIndex(Person person, Person coauthor)
            throws NullPointerException {
        return getCoauthorCommunityIndex(person, coauthor, true);
    }

    /**
     * Retrieves a list of person that form path of coauthors between the two given authors using a
     * minimal number of edges. If no such path exists, an empty list is returned.
     * <p>
     * <strong>Note:</strong> This is a very expensive operation, so handle with care!
     *
     * @param first The first person.
     * @param second The second person.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor nodes
     *            that link the network. Otherwise, disambiguation profiles are considered to have
     *            no outgoing coauthors.
     * @return The path as list of persons, or an empty list if no such path exists.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    public List<Person> getShortestCoauthorPath(Person first, Person second,
            boolean allowDisambiguations);

    /**
     * Retrieves a list of person that form path of coauthors between the two given authors using a
     * minimal number of edges. If no such path exists, an empty list is returned. Disambiguation
     * profiles are considered to be valid coauthor nodes that link the network.
     * <p>
     * <strong>Note:</strong> This is a very expensive operation, so handle with care!
     *
     * @param first The first person.
     * @param second The second person.
     * @return The path as list of persons, or an empty list if no such path exists.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    public default List<Person> getShortestCoauthorPath(Person first, Person second) {
        return getShortestCoauthorPath(first, second, true);
    }

    /**
     * Retrieve the local coauthor graph of a given person.
     *
     * @param person The person.
     * @return The coauthors of this community.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor nodes
     *            that link the network. Otherwise, disambiguation profiles are considered to have
     *            no coauthors and end up as singleton cluster nodes in the network.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public LocalCoauthorNetwork getCoauthorNetwork(Person person, boolean allowDisambiguations)
            throws NullPointerException;

    /**
     * Retrieve the local coauthor graph of a given person. Disambiguation profiles are considered
     * to be valid coauthor nodes that link the network.
     *
     * @param person The person.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    public default LocalCoauthorNetwork getCoauthorNetwork(Person person)
            throws NullPointerException {
        return getCoauthorNetwork(person, true);
    }

    // ------------------------------------------------------------
    // Table of Contents
    // ------------------------------------------------------------

    /**
     * Retrieves an unmodifiable Collection view of all table of contents in dblp.
     *
     * @return The table of contents.
     */
    public Collection<TableOfContents> getTocs();

    /**
     * Retrieves a sequential stream of all table of contents in dblp.
     *
     * @return The stream of booktitles.
     */
    public Stream<TableOfContents> tocs();

    /**
     * Retrieve the number of table of contents in dblp.
     *
     * @return The number of tocs.
     */
    public int numberOfTocs();

    // ------------------------------------------------------------
    // Booktitles
    // ------------------------------------------------------------

    /**
     * Checks whether a booktitle object with the given title string is present in dblp.
     *
     * @param title The title string.
     * @return {@code true} if the booktitle with the specified title is present, otherwise
     *         {@code false} .
     * @throws NullPointerException if the specified title is {@code null}.
     */
    default public boolean hasBookTitle(String title) throws NullPointerException {
        return getBookTitle(title) != null;
    }

    /**
     * Retrieve the booktitle object with the given booktitle string.
     *
     * @param title The title string.
     * @return The booktitle object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    public BookTitle getBookTitle(String title) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all booktitles in dblp.
     *
     * @return The booktitles.
     */
    public Collection<BookTitle> getBookTitles();

    /**
     * Retrieves a sequential stream of all booktitles in dblp.
     *
     * @return The stream of booktitles.
     */
    public Stream<BookTitle> bookTitles();

    // ------------------------------------------------------------
    // Journals
    // ------------------------------------------------------------

    /**
     * Checks whether a journal object with the given title string is present in dblp.
     *
     * @param title The title string.
     * @return {@code true} if the journal with the specified title is present, otherwise
     *         {@code false} .
     * @throws NullPointerException if the specified title is {@code null}.
     */
    default public boolean hasJournal(String title) throws NullPointerException {
        return getJournal(title) != null;
    }

    /**
     * Retrieve the journal object with the given journal title string.
     *
     * @param title The title string.
     * @return The journal object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    public JournalTitle getJournal(String title) throws NullPointerException;

    /**
     * Retrieves an unmodifiable Collection view of all journals in dblp.
     *
     * @return The journals.
     */
    public Collection<JournalTitle> getJournals();

    /**
     * Retrieves a sequential stream of all journals in dblp.
     *
     * @return The stream of journals.
     */
    public Stream<JournalTitle> journals();

    // ------------------------------------------------------------
    // Publication stream venues
    // ------------------------------------------------------------

    /**
     * Checks whether a publication stream object with the given title string is present in dblp.
     *
     * @param title The title string.
     * @return {@code true} if the stream with the specified title is present, otherwise
     *         {@code false} .
     * @throws NullPointerException if the specified title is {@code null}.
     */
    default public boolean hasPublicationStream(String title) throws NullPointerException {
        return getPublicationStream(title) != null;
    }

    /**
     * Retrieve the publication stream object with the given title string.
     *
     * @param title The title string.
     * @return The publication stream object, or {@code null} if there is no such title.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    default public PublicationStreamTitle getPublicationStream(String title)
            throws NullPointerException {
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
    default public Collection<PublicationStreamTitle> getPublicationStreams() {
        return publicationStreams().collect(Collectors.toList());
    }

    /**
     * Retrieves a combined, sequential stream of all publication stream venues (i.e., all
     * booktitles and all journals) in dblp.
     *
     * @return The stream of all publication stream venues.
     */
    default public Stream<PublicationStreamTitle> publicationStreams() {
        return Stream.concat(bookTitles(), journals());
    }

}
