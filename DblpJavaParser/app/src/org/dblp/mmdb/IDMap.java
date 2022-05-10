package org.dblp.mmdb;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.xml.sax.SAXException;


/**
 * A data structure for mapping external IDs to persons and publications.
 *
 * @author mra
 */
class IDMap {

    /** The MMDB. */
    private final MmdbInterface mmdb;
    /** The ID-to-publication-object maps. */
    private final Map<PublicationIDType, Map<String, Publication>> publIDMaps;
    /** The ID-to-person-object maps. */
    private final Map<PersonIDType, Map<String, Person>> persIDMaps;

    /**
     * Create a new ID map data structure from the given MMDB.
     *
     * @param mmdb The MMDB.
     */
    IDMap(MmdbInterface mmdb) {

        this.mmdb = mmdb;

        // init maps
        long start = System.currentTimeMillis();
        this.publIDMaps = new TreeMap<>();
        for (PublicationIDType type : PublicationIDType.values()) {
            this.publIDMaps.put(type, new TreeMap<>());
        }
        this.persIDMaps = new TreeMap<>();
        for (PersonIDType type : PersonIDType.values()) {
            this.persIDMaps.put(type, new TreeMap<>());
        }
        // map ID to publications
        for (Publication publ : mmdb.getPublications()) {
            FieldReader fields = publ.getFieldReader();
            for (Field eeField : fields.getFields("ee")) {
                String url = eeField.value();
                PublicationIDType type = PublicationIDType.of(url);
                if (type != null) this.publIDMaps.get(type).put(type.getID(url), publ);
            }
            for (Field isbnField : fields.getFields("isbn")) {
                String isbn = PublicationIDType.ISBN.normalize(isbnField.value());
                this.publIDMaps.get(PublicationIDType.ISBN).put(isbn, publ);
            }
        }
        // map ID to persons
        for (Person pers : mmdb.getPersons()) {
            FieldReader fields = pers.getFieldReader();
            for (Field urlField : fields.getFields("url")) {
                String url = urlField.value();
                PersonIDType type = PersonIDType.of(url);
                if (type != null) this.persIDMaps.get(type).put(type.getID(url), pers);
            }
        }
        long end = System.currentTimeMillis();
        Mmdb.LOG.info("build ID maps time", (end - start) + " ms");
    }

    /**
     * Retrieve the publication with the given external ID.
     *
     * @param type The ID type.
     * @param id The ID of the publication to be retrieved.
     * @return The publication, or {@code null} if there is no such publication.
     * @throws NullPointerException if the specified type or ID is {@code null}.
     */
    Publication getPublication(PublicationIDType type, String id) throws NullPointerException {
        if (type != PublicationIDType.DBLP)
            return this.publIDMaps.get(type).get(type.normalize(id));
        return this.mmdb.getPublication(id);
    }

    /**
     * Retrieves an unmodifiable Collection view of the publications in the MMDB which are decorated
     * with an external ID of the given type.
     *
     * @param type The ID type.
     * @return The publications.
     */
    Collection<Publication> getPublications(PublicationIDType type) {
        if (type != PublicationIDType.DBLP)
            return Collections.unmodifiableCollection(this.publIDMaps.get(type).values());
        return this.mmdb.getPublications();
    }

    /**
     * Retrieves a sequential stream of the publications in the MMDB which are decorated with an
     * external ID of the given type.
     *
     * @param type The ID type.
     * @return The stream of publications.
     */
    Stream<Publication> publications(PublicationIDType type) {
        if (type != PublicationIDType.DBLP) return this.publIDMaps.get(type).values().stream();
        return this.mmdb.publications();
    }

    /**
     * Retrieve the number of publications in the MMDB which are decorated with an external ID of
     * the given type.
     *
     * @param type The ID type.
     * @return The number of publications.
     */
    int numberOfPublications(PublicationIDType type) {
        if (type != PublicationIDType.DBLP) return this.publIDMaps.get(type).size();
        return this.mmdb.numberOfPublications();
    }

    /**
     * Retrieve the person with the given external ID.
     *
     * @param type The ID type.
     * @param id The ID of the person to be retrieved.
     * @return The person, or {@code null} if there is no such person.
     * @throws NullPointerException if the specified type or ID is {@code null}.
     */
    Person getPerson(PersonIDType type, String id) throws NullPointerException {
        if (type != PersonIDType.DBLP) return this.persIDMaps.get(type).get(type.normalize(id));
        return this.mmdb.getPerson("homepages/" + id);
    }

    /**
     * Retrieves an unmodifiable Collection view of the persons in the MMDB which are decorated with
     * an external ID of the given type.
     *
     * @param type The ID type.
     * @return The persons.
     */
    Collection<Person> getPersons(PersonIDType type) {
        if (type != PersonIDType.DBLP)
            return Collections.unmodifiableCollection(this.persIDMaps.get(type).values());
        return this.mmdb.getPersons();
    }

    /**
     * Retrieves a sequential stream of all persons in the MMDB which are decorated with an external
     * ID of the given type.
     *
     * @param type The ID type.
     * @return The stream of persons.
     */
    Stream<Person> persons(PersonIDType type) {
        if (type != PersonIDType.DBLP) return this.persIDMaps.get(type).values().stream();
        return this.mmdb.persons();
    }

    /**
     * Retrieve the number of persons in the MMDB which are decorated with an external ID of the
     * given type.
     *
     * @param type The ID type.
     * @return The number of persons.
     */
    int numberOfPersons(PersonIDType type) {
        if (type != PersonIDType.DBLP) { return this.persIDMaps.get(type).values().size(); }
        return this.mmdb.numberOfPersons();
    }

    // TODO: testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws IOException, SAXException {

        if (args.length < 2) {
            System.out.format("Usage: %s <dblp.xml> <dblp.dtd>\n", IDMap.class.getSimpleName());
            System.exit(0);
        }

        MmdbInterface dblp = new Mmdb(args[0], args[1], false);

        System.out.println("dblp.numberOfPublications() = " + dblp.numberOfPublications());
        System.out.println("dblp.numberOfPersons() = " + dblp.numberOfPersons());
        System.out.println();

        IDMap ids = new IDMap(dblp);
        for (PublicationIDType type : PublicationIDType.values()) {
            int num = ids.numberOfPublications(type);
            if (num == 0) continue;
            System.out.println("number of " + type.label() + "s: " + num);
        }
        for (PersonIDType type : PersonIDType.values()) {
            int num = ids.numberOfPersons(type);
            if (num == 0) continue;
            System.out.println("number of " + type.label() + "s: " + num);
        }
    }

}
