package org.dblp.mmdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A class representing dblp person records.
 */
public class Person extends Record implements Comparable<Person> {

    /** Empty XML byte array template for building new Person records. */
    protected static final byte[] defaultPersonRecord = "<person><0/></person>".getBytes(UTF8);
    /** An empty Publication array, used as default return value. < */
    private static final Publication[] emptyPublsArray = new Publication[0];

    /** The publications of this person. */
    private Publication publs[];
    /**
     * The aggregated mdate of this record and all attached publications Uses the same integer
     * representation as {@link #mdate}: String <code>"yyyy-mm-dd"</code> is stores as integer
     * <pre>mdate = yyyy * 10000 + mm * 100 + dd</pre>.
     */
    private int aggrMdate;
    /** The formatted aggregated mdate string, initialized lazy from {@link #aggrMdate}. */
    private String aggrMdateString;
    /**
     * Internal count of the number of missing publications to be linked to this person record. This
     * field is only used during the construction of the MMDB graph.
     */
    private int count;

    /**
     * Create a new, empty Person container. This constructor is intended only for extensions, and
     * the implementing class has to take care of initializing all member fields properly.
     */
    protected Person() {}

    /**
     * Create a new Person.
     *
     * @param dblp The MMDB.
     * @param key The record key.
     * @param mdate The record mdate.
     * @param xml The dblp record as byte array, or <code>null</code> if an empty default template
     *            should be used.
     * @param names The PersonNames referenced in this record.
     * @throws IllegalArgumentException if nor valid name is given.
     */
    Person(MmdbInterface dblp, String key, int mdate, byte[] xml, PersonName[] names)
            throws IllegalArgumentException {
        // protected Person(String key, int mdate, byte[] xml, PersonName[] names) {
        if (names == null || names.length == 0)
            throw new IllegalArgumentException("cannot instantiate Person without a name");

        this.dblp = dblp;
        this.key = key;
        this.mdate = mdate;
        this.aggrMdate = mdate;
        if (xml == null) xml = defaultPersonRecord;
        this.xml = xml;
        this.names = names;
        this.count = 0;
        this.publs = null;
    }

    /**
     * Return the internal count of the number of missing publications to be linked to this person
     * record. This value is only used during the construction of the MMDB graph.
     *
     * @return The count.
     */
    int count() {
        return this.count;
    }

    /**
     * Increment the internal count of the number of missing publications to be linked to this
     * person record. This method is only used during the construction of the MMDB graph.
     */
    void incr() {
        this.count++;
    }

    /**
     * Retrieve the PID of this person record.
     *
     * @return The PID.
     */
    public String getPid() {
        if (!this.key.startsWith("homepages/"))
            throw new IllegalStateException("person record has invalid key: " + this.key);

        return this.key.substring(10);
    }

    /**
     * Retrieves all external ID types stored in this person record. The internal type
     * {@link PersonIDType#DBLP} will be ignored by this method.
     *
     * @return The types.
     */
    public Set<PersonIDType> getIdTypes() {
        Set<PersonIDType> result = new HashSet<>(4);
        for (Field urlField : this.getFields("url")) {
            PersonIDType type = PersonIDType.of(urlField.value());
            if (type != null && type != PersonIDType.DBLP) result.add(type);
        }
        return result;
    }

    /**
     * Retrieves all external IDs stored in this person record matching the given type. Querying for
     * {@link PersonIDType#DBLP} will just return a single-item list with the same ID result as
     * {@link #getPid()}.
     *
     * @param type The ID type.
     * @return The IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public List<String> getIds(PersonIDType type) {
        if (type == null) throw new NullPointerException();
        if (type == PersonIDType.DBLP) return Arrays.asList(this.getPid());

        List<String> result = new ArrayList<>(2);
        for (Field urlField : this.getFields("url")) {
            PersonIDType urlType = PersonIDType.of(urlField.value());
            if (urlType == type) result.add(urlType.getID(urlField.value()));
        }
        return result;
    }

    /**
     * Retrieves a sequential stream of all external ID types stored in this person record. The
     * internal type {@link PersonIDType#DBLP} will be ignored by this method.
     *
     * @return The stream of types.
     */
    public Stream<PersonIDType> idTypes() {
        return this.fields("url").map(field -> field.value()).map(PersonIDType::of).filter(Objects::nonNull).filter(type -> !type.equals(PersonIDType.DBLP));
    }

    /**
     * Retrieves a sequential stream of all external IDs stored in this person record matching the
     * given type. Querying for {@link PersonIDType#DBLP} will just return a single-item list with
     * the same ID result as {@link #getPid()}.
     *
     * @param type The ID type.
     * @return The stream of IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    public Stream<String> ids(PersonIDType type) {
        if (type == null) throw new NullPointerException();
        if (type == PersonIDType.DBLP) return Stream.of(this.getPid());

        return this.fields("url").filter(field -> type.equals(PersonIDType.of(field.value()))).map(field -> PersonIDType.of(field.value()).getID(field.value()));
    }

    /**
     * Retrieve the most recent mdate String for this record and all attached publications.
     *
     * @return The aggregated mdate.
     */
    public String getAggregatedMdate() {
        if (this.aggrMdateString == null) {
            int yyyy = this.aggrMdate / 10000;
            int mm = (this.aggrMdate % 10000) / 100;
            int dd = this.aggrMdate % 100;
            this.aggrMdateString = String.format("%04d-%02d-%02d", yyyy, mm, dd).intern();
        }
        return this.aggrMdateString;
    }

    /**
     * Links a publication to this person.
     *
     * @param publ The publication to add.
     */
    // FIXME: Change to ArrayList?
    void addPublication(Publication publ) {
        // FIXME: what happens if count <= 0?
        // if( count <= 0 ) throw new IllegalStateException();
        if (this.publs == null && this.count > 0) {
            this.publs = new Publication[this.count];
        }
        if (this.count > 0) this.publs[--this.count] = publ;
        if (this.aggrMdate < publ.mdate) this.aggrMdate = publ.mdate;
    }

    /**
     * Retrieves an unmodifiable list view of all publication by this person.
     *
     * @return The publications as unmodifiable list.
     */
    public List<Publication> getPublications() {
        if (this.publs == null) this.publs = emptyPublsArray;
        return Collections.unmodifiableList(Arrays.asList(this.publs));
    }

    /**
     * Returns a sequential stream of all publications by this person.
     *
     * @return The stream of publications.
     */
    public Stream<Publication> publications() {
        return getPublications().stream();
    }

    /**
     * Get the number of publications of this person.
     *
     * @return The number of publications.
     */
    public int numberOfPublications() {
        return getPublications().size();
    }

    /**
     * Get the primary name of this person.
     *
     * @return The person name.
     */
    public PersonName getPrimaryName() {
        // if (this.names == null || this.names.length == 0) return null;
        return this.names[0];
    }

    /**
     * Checks whether this person record carries a certain dblp name.
     *
     * @param name The complete name string to look up, including any possible homonym numbers.
     * @return {@code true} if this person has the given name, otherwise {@code false}.
     */
    public boolean hasName(String name) {
        for (PersonName pname : this.names) {
            if (pname.name().equals(name)) return true;
        }
        return false;
    }

    /**
     * Checks whether this person record carries a certain person name.
     *
     * @param name The person name to look up.
     * @return {@code true} if this person has the given name, otherwise {@code false}.
     */
    public boolean hasName(PersonName name) {
        Person other = name.getPerson();
        if (other != null) return this.equals(other);

        for (PersonName alias : this.names) {
            if (alias.name().equals(name.name())) return true;
        }
        return false;
    }

    /**
     * Checks whether this person record is trivial, i.e., the record does not contain any
     * information beside a single person name. In particular, the record does <em>not</em> include:
     * <ul>
     * <li>attributes
     * <li>alias names
     * <li>home page URLs
     * <li>notes
     * <li>cite references
     * <li>is-not references
     * </ul>
     *
     * @return {@code true} if this person record is trivial, or {@code false} if any non-trivial
     *         information is present.
     */
    public boolean isTrivial() {
        if (this.getAttributes().size() > 2) return false;
        if (this.hasAliases()) return false;
        if (this.hasPersonInfo()) return false;
        if (this.hasIsNotPersons()) return false;
        return true;
    }

    /**
     * Checks whether this person labeled as an unlisted profile.
     *
     * @return {@code true} if this person profile is unlisted, otherwise {@code false}.
     */
    public boolean isNoShow() {
        Map<String, String> attr = this.getAttributes();
        if (attr.containsKey("publtype") && attr.get("publtype").contains("noshow")) return true;
        return false;
    }

    /**
     * Checks whether this person should be considered a disambiguation pseudo person. If
     * {@code checkHeuristically == true} then this method will also perform heuristic checks.
     * <p>
     * The heuristic check consists of checking whether a homonymous name with id '0001' exists in
     * dblp.
     *
     * @param checkHeuristically Whether to check heuristically.
     * @return {@code true} if this person should be considered a disambiguation pseudo person,
     *         otherwise {@code false}.
     */
    public boolean isDisambiguation(boolean checkHeuristically) {
        Map<String, String> attr = this.getAttributes();
        if (attr.containsKey("publtype") && attr.get("publtype").contains("disambiguation")) {
            return true;
        }
        else if (checkHeuristically) {
            for (PersonName homName : this.getNames()) {
                if (this.dblp.getPersonName(homName.name() + " 0001") != null) return true;
            }
        }
        return false;
    }

    /**
     * Checks (explicitly and heuristically) whether this person should be considered a
     * disambiguation pseudo person.
     * <p>
     * The heuristic check consists of checking whether a homonymous name with id '0001' exists in
     * dblp.
     *
     * @return {@code true} if this person should be considered a disambiguation pseudo person,
     *         otherwise {@code false}.
     */
    public boolean isDisambiguation() {
        return this.isDisambiguation(true);
    }

    /**
     * Checks whether this person should be considered a group pseudo person.
     *
     * @return {@code true} if this person should be considered a group pseudo person, otherwise
     *         {@code false}.
     */
    public boolean isGroup() {
        Map<String, String> attr = this.getAttributes();
        if (attr.containsKey("publtype") && attr.get("publtype").contains("group")) return true;
        return false;
    }

    /**
     * Checks whether this person record has alias names.
     *
     * @return {@code true} if this person has alias names, otherwise {@code false}.
     */
    public boolean hasAliases() {
        return this.names.length > 1;
    }

    /**
     * Checks whether this person record contains additional person information. This includes:
     * <ul>
     * <li>home page URLs,
     * <li>notes (including is-not references), or
     * <li>cite references
     * </ul>
     *
     * @return {@code true} if this person contains additional information, otherwise {@code false}.
     */
    public boolean hasPersonInfo() {
        FieldReader fields = this.getFieldReader();
        if (fields.contains("url")) return true;
        if (fields.contains("note")) return true;
        if (fields.contains("cite")) return true;
        return false;
    }

    /**
     * Checks whether this person has explicitly disambiguated persons listed (i.e., entries in an
     * {@code <note type="isnot">} element).
     *
     * @return {@code true} if this person has explicitly disambiguated persons listed, otherwise
     *         {@code false}.
     */
    public boolean hasIsNotPersons() {
        for (Field noteField : this.getFields("note")) {
            String noteType = noteField.attribute("type");
            if (noteType != null && noteType.equals("isnot")) return true;
        }
        return false;
    }

    /**
     * Retrieves an unmodifiable list view of all explicitly disambiguated person names of this
     * person (i.e., all entries of the {@code <note type="isnot">} elements, if any).
     *
     * @return The explicitly disambiguated person names as unmodifiable list.
     */
    public List<PersonName> getIsNotPersonNames() {
        List<PersonName> result = new ArrayList<>(0);
        for (Field noteField : this.getFields("note")) {
            String noteType = noteField.attribute("type");
            if (noteType != null && noteType.equals("isnot")) {
                PersonName name = this.dblp.getPersonName(noteField.value());
                if (name != null) result.add(name);
                else Mmdb.LOG.warn("no such is-not target", "'" + noteField.value() + "' in"
                        + this.getKey());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Retrieves an unmodifiable list view of all explicitly disambiguated persons of this person
     * (i.e., all entries of the {@code <note type="isnot">} elements, if any).
     *
     * @return The explicitly disambiguated persons as unmodifiable list.
     */
    public List<Person> getIsNotPersons() {
        List<PersonName> isNotNames = getIsNotPersonNames();
        List<Person> result =
                isNotNames.stream().map(PersonName::getPerson).filter(Objects::nonNull).collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }

    @Override
    public String getXml() {
        return xmlBuild().toString();
    }

    @Override
    public int compareTo(Person other) {
        return this.getPrimaryName().compareTo(other.getPrimaryName());
    }
}
