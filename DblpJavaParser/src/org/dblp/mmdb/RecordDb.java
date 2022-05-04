package org.dblp.mmdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.xml.sax.SAXException;


/**
 * An in-memory data structure for dblp's publication and person record corpus. It parses the
 * {@code dblp.xml}, a concatenated version of all dblp records in the {@code /dblp/publ} tree.
 *
 * @author mra
 */
public class RecordDb implements RecordDbInterface {

    /** The MMDB. */
    private final Mmdb mmdb;
    /** The homonym name mapping. */
    private HomonymMap homMap;
    /** The external ID maps. */
    private IDMap idMap;
    /** The coauthor graph. */
    private final LazyCoauthorGraph graph;
    /** Stores whether the lazy coauthor graph has been build. */
    private boolean graphIsBuild;

    /**
     * Constructs the record DB for the given dblp records from the input stream.
     *
     * @param xmlInputStream The dblp XML records to be loaded into the main memory database.
     * @param dtdInputStream The content of the dblp.dtd. If set to {@code null}, the dblp.dtd file
     *            needs to be available in the classpath.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws IOException if any IO errors occur while parsing the XML stream.
     * @throws SAXException if any SAX errors occur while parsing the XML stream.
     */
    public RecordDb(InputStream xmlInputStream, InputStream dtdInputStream, boolean verbose)
            throws IOException, SAXException {

        this.mmdb = new Mmdb(xmlInputStream, dtdInputStream, verbose);
        this.graph = new LazyCoauthorGraph(this.mmdb);
        this.graphIsBuild = false;
    }

    /**
     * Constructs the main memory database for the given dblp records from the input stream. The
     * dblp.dtd file needs to be available in the classpath.
     *
     * @param xmlInputStream The dblp XML records to be loaded into the main memory database.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws IOException if any IO errors occur while parsing the XML stream.
     * @throws SAXException if any SAX errors occur while parsing the XML stream.
     */
    public RecordDb(InputStream xmlInputStream, boolean verbose) throws IOException, SAXException {
        this(xmlInputStream, null, verbose);
    }

    /**
     * Constructs the main memory database for the given dblp file.
     *
     * @param xmlFilename The dblp XML file to be loaded into the main memory database.
     * @param dtdFilename The dblp.dtd file.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws NullPointerException if a file is given as {@code null}.
     * @throws FileNotFoundException if a given file cannot be opened for reading.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    public RecordDb(String xmlFilename, String dtdFilename, boolean verbose)
            throws NullPointerException, FileNotFoundException, IOException, SAXException {
        this(xmlFilename.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(xmlFilename))
                : new FileInputStream(new File(xmlFilename)), new FileInputStream(new File(dtdFilename)), verbose);
    }

    /**
     * Constructs the main memory database for the given dblp file. The dblp.dtd file needs to be
     * available in the classpath.
     *
     * @param xmlFilename The dblp XML file to be loaded into the main memory database.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws NullPointerException if {@code xmlFilename == null}.
     * @throws FileNotFoundException if {@code xmlFilename} cannot be opened for reading.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    public RecordDb(String xmlFilename, boolean verbose)
            throws NullPointerException, FileNotFoundException, IOException, SAXException {
        this(xmlFilename.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(xmlFilename))
                : new FileInputStream(new File(xmlFilename)), null, verbose);
    }

    /**
     * Static factory method to constructs the main memory database for a gzipped dblp XML input.
     *
     * @param gzipInputStream The gzipped dblp XML file to be loaded into the main memory database.
     * @param dtdInputStream The dblp.dtd file.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @return The record DB.
     * @throws ZipException if a GZIP format error occurs or the compression method used is
     *             unsupported.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    public static RecordDb fromGzip(InputStream gzipInputStream, InputStream dtdInputStream,
            boolean verbose)
            throws ZipException, IOException, SAXException {
        InputStream xmlInputStream = new GZIPInputStream(gzipInputStream);
        return new RecordDb(xmlInputStream, dtdInputStream, verbose);
    }

    /**
     * Static factory method to constructs the main memory database for a gzipped dblp XML file.
     *
     * @param gzipFilename The gzipped dblp XML file to be loaded into the main memory database.
     * @param dtdFilename The dblp.dtd file.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @return The record DB.
     * @throws NullPointerException if a file is given as {@code null}.
     * @throws FileNotFoundException if a given file cannot be opened for reading.
     * @throws ZipException if a GZIP format error occurs or the compression method used is
     *             unsupported.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    public static RecordDb fromGzip(String gzipFilename, String dtdFilename, boolean verbose)
            throws NullPointerException, FileNotFoundException, ZipException, IOException,
            SAXException {
        return fromGzip(new FileInputStream(new File(gzipFilename)), new FileInputStream(new File(dtdFilename)), verbose);
    }

    // ensure lazy building

    @Override
    public void ensureHomonymMap() {
        if (this.homMap == null) this.homMap = new HomonymMap(this.mmdb);
    }

    @Override
    public void ensureIdMap() {
        if (this.idMap == null) this.idMap = new IDMap(this.mmdb);
    }

    @Override
    public void ensureCoauthorGraph() {
        if (!this.graphIsBuild) {
            long start = System.currentTimeMillis();
            this.mmdb.persons().forEach(pers -> this.graph.ensurePerson(pers));
            long end = System.currentTimeMillis();
            Mmdb.LOG.info("build coauthor graphs time", (end - start) + " ms");
            this.graphIsBuild = true;
        }
    }

    // publications

    @Override
    public Publication getPublication(String key) throws NullPointerException {
        if (key == null) throw new NullPointerException();
        return this.mmdb.getPublication(key);
    }

    @Override
    public Collection<Publication> getPublications() {
        return this.mmdb.getPublications();

    }

    @Override
    public Stream<Publication> publications() {
        return this.mmdb.publications();
    }

    @Override
    public int numberOfPublications() {
        return this.mmdb.numberOfPublications();
    }

    @Override
    public Publication getPublication(PublicationIDType type, String id)
            throws NullPointerException {
        if (type == null || id == null) throw new NullPointerException();
        if (this.idMap == null) ensureIdMap();
        return this.idMap.getPublication(type, id);
    }

    @Override
    public Collection<Publication> getPublications(PublicationIDType type) {
        if (type == null) throw new NullPointerException();
        if (this.idMap == null) ensureIdMap();
        return this.idMap.getPublications(type);
    }

    @Override
    public Stream<Publication> publications(PublicationIDType type) {
        if (type == null) throw new NullPointerException();
        if (this.idMap == null) this.idMap = new IDMap(this.mmdb);
        return this.idMap.publications(type);
    }

    @Override
    public int numberOfPublications(PublicationIDType type) {
        if (type == null) throw new NullPointerException();
        if (this.idMap == null) this.idMap = new IDMap(this.mmdb);
        return this.idMap.numberOfPublications(type);
    }

    // persons

    @Override
    public Person getPerson(String key) throws NullPointerException {
        if (key == null) throw new NullPointerException();

        Person pers = this.mmdb.getPerson(key);
        if (pers == null) {
            Redirect redir = this.mmdb.getRedirect(key);
            try {
                pers = redir.getTarget();
            }
            catch (NullPointerException | IllegalStateException ex) {
                return null;
            }
        }
        return pers;
    }

    @Override
    public Collection<Person> getPersons() {
        return this.mmdb.getPersons();
    }

    @Override
    public Stream<Person> persons() {
        return this.mmdb.persons();
    }

    @Override
    public int numberOfPersons() {
        return this.mmdb.numberOfPersons();
    }

    @Override
    public Person getPerson(PersonIDType type, String id) throws NullPointerException {
        if (type == null || id == null) throw new NullPointerException();
        if (this.idMap == null) ensureIdMap();
        return this.idMap.getPerson(type, id);
    }

    @Override
    public Collection<Person> getPersons(PersonIDType type) {
        if (type == null) throw new NullPointerException();
        if (this.idMap == null) ensureIdMap();
        return this.idMap.getPersons(type);
    }

    @Override
    public Stream<Person> persons(PersonIDType type) {
        if (type == null) throw new NullPointerException();
        if (this.idMap == null) this.idMap = new IDMap(this.mmdb);
        return this.idMap.persons(type);
    }

    @Override
    public int numberOfPersons(PersonIDType type) {
        if (type == null) throw new NullPointerException();
        if (this.idMap == null) this.idMap = new IDMap(this.mmdb);
        return this.idMap.numberOfPersons(type);
    }

    // person names

    @Override
    public PersonName getPersonName(String name) throws NullPointerException {
        if (name == null) throw new NullPointerException();
        try {
            return this.mmdb.getPersonName(name);
        }
        catch (NullPointerException ex) {
            return null;
        }
    }

    @Override
    public Collection<PersonName> getPersonNames() {
        return this.mmdb.getPersonNames();
    }

    @Override
    public Stream<PersonName> personNames() {
        return this.mmdb.personNames();
    }

    @Override
    public int numberOfPersonNames() {
        return this.mmdb.numberOfPersonNames();
    }

    // homonyms

    @Override
    public Collection<PersonName> getAllHomonymsByName(String name) throws NullPointerException {
        if (this.homMap == null) ensureHomonymMap();
        return this.homMap.getHomonyms(name);
    }

    @Override
    public Stream<PersonName> allHomonymsByName(String name) throws NullPointerException {
        if (this.homMap == null) ensureHomonymMap();
        return this.homMap.homonyms(name);
    }

    @Override
    public int numberOfAllHomonymsByName(String name) {
        if (this.homMap == null) ensureHomonymMap();
        return this.homMap.numberOfHomonyms(name);
    }

    // coauthors

    @Override
    public Collection<Person> getCoauthors(Person person) throws NullPointerException {
        if (person == null) throw new NullPointerException();
        return this.graph.getCoauthors(person);
    }

    @Override
    public Stream<Person> coauthors(Person person) throws NullPointerException {
        if (person == null) throw new NullPointerException();
        return this.graph.coauthors(person);
    }

    @Override
    public int numberOfCoauthors(Person person) throws NullPointerException {
        if (person == null) throw new NullPointerException();
        return this.graph.numberOfCoauthors(person);
    }

    @Override
    public int numberOfCoauthorCommunities(Person person, boolean allowDisambiguations)
            throws NullPointerException {
        if (person == null) throw new NullPointerException();
        return this.graph.numberOfCoauthorCommunities(person, allowDisambiguations);
    }

    @Override
    public Collection<Person> getCoauthorCommunity(Person person, int index,
            boolean allowDisambiguations)
            throws NullPointerException, IndexOutOfBoundsException {
        if (person == null) throw new NullPointerException();
        return this.graph.getCoauthorCommunity(person, index, allowDisambiguations);
    }

    @Override
    public boolean hasCoauthors(Person first, Person second) throws NullPointerException {
        if (first == null || second == null) throw new NullPointerException();
        return this.graph.hasCoauthors(first, second);
    }

    @Override
    public int numberOfCoPublications(Person first, Person second) throws NullPointerException {
        if (first == null || second == null) throw new NullPointerException();
        return (int) this.graph.weight(first, second);
    }

    @Override
    public List<Person> getShortestCoauthorPath(Person first, Person second,
            boolean allowDisambiguations) {
        if (first == null || second == null) throw new NullPointerException();
        return this.graph.shortestPath(first, second, allowDisambiguations);
    }

    @Override
    public LocalCoauthorNetwork getCoauthorNetwork(Person person, boolean allowDisambiguations)
            throws NullPointerException {
        if (person == null) throw new NullPointerException();
        return this.graph.getCoauthorGraph(person, allowDisambiguations);
    }

    // tocs

    @Override
    public TableOfContents getToc(String key) throws NullPointerException {
        if (key == null) throw new NullPointerException();
        return this.mmdb.getToc(key);
    }

    @Override
    public Collection<TableOfContents> getTocs() {
        return this.mmdb.getTocs();
    }

    @Override
    public Stream<TableOfContents> tocs() {
        return this.mmdb.tocs();
    }

    @Override
    public int numberOfTocs() {
        return this.mmdb.numberOfTocs();
    }

    // book titles

    @Override
    public BookTitle getBookTitle(String title) throws NullPointerException {
        if (title == null) throw new NullPointerException();
        return this.mmdb.getBookTitle(title);
    }

    @Override
    public Collection<BookTitle> getBookTitles() {
        return this.mmdb.getBookTitles();
    }

    @Override
    public Stream<BookTitle> bookTitles() {
        return this.mmdb.bookTitles();
    }

    // journal titles

    @Override
    public JournalTitle getJournal(String title) throws NullPointerException {
        if (title == null) throw new NullPointerException();
        return this.mmdb.getJournal(title);
    }

    @Override
    public Collection<JournalTitle> getJournals() {
        return this.mmdb.getJournals();
    }

    @Override
    public Stream<JournalTitle> journals() {
        return this.mmdb.journals();
    }

    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws NullPointerException, IOException, SAXException {

        if (args.length < 2) {
            System.out.format("Usage: %s <dblp.xml> <dblp.dtd>\n", RecordDb.class.getSimpleName());
            System.exit(0);
        }

        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        RecordDbInterface dblp;
        if (dblpXmlFilename.toLowerCase().endsWith(".gz")) {
            dblp = RecordDb.fromGzip(dblpXmlFilename, dblpDtdFilename, true);
        }
        else dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, true);

        System.out.println("dblp.numberOfPublications() = " + dblp.numberOfPublications());
        System.out.println("dblp.numberOfPersons() = " + dblp.numberOfPersons());
        System.out.println();

        Person wei = dblp.getPerson("homepages/w/WeiWang11");
        System.out.println("dblp.numberOfAllHomonyms(wei) = "
                + dblp.numberOfAllHomonyms(wei.getPrimaryName()));
        System.out.println("dblp.numberOfAllHomonyms('Wei Wang') = "
                + dblp.numberOfAllHomonymsByName(wei.getPrimaryName().name(false)));
        System.out.println("dblp.allHomonyms(wei) = "
                + wei.names().flatMap(name -> dblp.allHomonyms(name)).map(PersonName::name).collect(Collectors.toList()));
        System.out.println();

        Person avi = dblp.getPerson("homepages/w/AviWigderson");
        Person noga = dblp.getPerson("homepages/a/NAlon");
        Person christos = dblp.getPerson("homepages/p/CHPapadimitriou");

        System.out.println("dblp.numberOfCoauthors(avi) = " + dblp.numberOfCoauthors(avi));
        System.out.println("dblp.hasCoauthors(noga,christos) = "
                + dblp.hasCoauthors(noga, christos));
        System.out.println("dblp.weight(noga,christos) = "
                + dblp.numberOfCoPublications(noga, christos));
        System.out.println("dblp.hasCoauthors(avi,noga) = " + dblp.hasCoauthors(avi, noga));
        System.out.println("dblp.weight(avi,noga) = " + dblp.numberOfCoPublications(avi, noga));
        System.out.println("dblp.hasCoauthors(avi,christos) = " + dblp.hasCoauthors(avi, christos));
        System.out.println("dblp.weight(avi,christos) = "
                + dblp.numberOfCoPublications(avi, christos));
        System.out.println();

        System.out.println(dblp.getShortestCoauthorPath(noga, christos).stream().map(Person::getPrimaryName).map(PersonName::name).collect(Collectors.joining(", ", "[", "]")));
        System.out.println(dblp.getShortestCoauthorPath(avi, wei).stream().map(Person::getPrimaryName).map(PersonName::name).collect(Collectors.joining(", ", "[", "]")));
        System.out.println();

        Person top = dblp.coauthors(noga).parallel().max((c1,
                c2) -> Double.compare(dblp.numberOfCoPublications(noga, c1), dblp.numberOfCoPublications(noga, c2))).orElse(null);
        System.out.format("argmax_x dblp.numberOfCoPublications(noga,x) = %s (%d)\n", top.getPrimaryName().name(), dblp.numberOfCoPublications(noga, top));

        top = dblp.coauthors(noga).parallel().max((c1,
                c2) -> Double.compare(dblp.numberOfCoPublications(c1, noga), dblp.numberOfCoPublications(c2, noga))).orElse(null);
        System.out.format("argmax_x dblp.numberOfCoPublications(x,noga) = %s (%d)\n", top.getPrimaryName().name(), dblp.numberOfCoPublications(top, noga));
        System.out.println();

        for (PublicationIDType type : PublicationIDType.values()) {
            int num = dblp.numberOfPublications(type);
            if (num == 0) continue;
            System.out.println("number of " + type.label() + "s: " + num);
        }
        for (PersonIDType type : PersonIDType.values()) {
            int num = dblp.numberOfPersons(type);
            if (num == 0) continue;
            System.out.println("number of " + type.label() + "s: " + num);
        }
        System.out.println();

        System.out.println("ORCID: 0000-0002-6148-9212");
        Person fellows = dblp.getPerson(PersonIDType.ORCID, "0000-0002-6148-9212");
        System.out.println(fellows.getXml());
        System.out.println();

        System.out.println("WikiData: Q4747864");
        Person fiat = dblp.getPerson(PersonIDType.WIKIDATA, "Q4747864");
        System.out.println(fiat.getXml());
        System.out.println();

        System.out.println("Wikipedia: H._V._Jagadish");
        Person hvj = dblp.getPerson(PersonIDType.WIKIPEDIA_EN, "H._V._Jagadish");
        System.out.println(hvj.getXml());
        System.out.println();

        System.out.println("Google: bAa___kAAAAJ");
        Person hgm = dblp.getPerson(PersonIDType.GOOGLE_SCHOLAR, "bAa___kAAAAJ");
        System.out.println(hgm.getXml());
        System.out.println();

        System.out.println("dblp: s/RalfSchenkel");
        Person schenkel = dblp.getPerson(PersonIDType.DBLP, "s/RalfSchenkel");
        System.out.println(schenkel.getXml());
        System.out.println();

        System.out.println("DOI: 10.1109/SWCT.1964.23");
        Publication bernstein64 =
                dblp.getPublication(PublicationIDType.DOI, "10.1109/SWCT.1964.23");
        System.out.println(bernstein64.getXml());
        System.out.println();

        System.out.println("URN: urn:nbn:de:0074-1671-7");
        Publication vldbPhd2016 =
                dblp.getPublication(PublicationIDType.URN_NBN_DE, "urn:nbn:de:0074-1671-7");
        System.out.println(vldbPhd2016.getXml());
        System.out.println();

        System.out.println("ISBN: 1558604707");
        Publication vldb97 = dblp.getPublication(PublicationIDType.ISBN, "1558604707");
        System.out.println(vldb97.getXml());
        System.out.println();

        System.out.println("dblp: conf/focs/Luby02");
        Publication luby02 = dblp.getPublication(PublicationIDType.DBLP, "conf/focs/Luby02");
        System.out.println(luby02.getXml());
        System.out.println();

    }

}
