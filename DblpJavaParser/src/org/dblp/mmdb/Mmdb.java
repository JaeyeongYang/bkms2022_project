package org.dblp.mmdb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * This class builds a simple main memory data structure for the dblp records. A MMDB object may
 * contain the complete dblp collection or a subset of it.
 *
 * @author ley
 */
public class Mmdb implements MmdbInterface {

    /** The logger. */
    static final MmdbLogger LOG = new MmdbLogger(MmdbLogLevel.WARN, System.err, 100);
    /** The charset to use. */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** The publication-key-to-publication-record map. */
    private final Map<String, Publication> publicationMap;
    /** The person-key-to-person-redirect-record map. */
    private final Map<String, Redirect> redirectMap;
    /** The name-to-person-name-object map. */
    private final Map<String, PersonName> persNameMap;
    /** The toc-key-to-toc-object map. */
    private final Map<String, TableOfContents> tocMap;
    /** The journal-title-to-journal-object map. */
    private final Map<String, JournalTitle> journalMap;
    /** The book-title-to-book-title-object map. */
    private final Map<String, BookTitle> booktitleMap;

    /** An empty person name array. */
    private static final PersonName[] EMPTY_PERSON_NAME_ARRAY = new PersonName[0];

    /**
     * The custom callback handler for SAX parsing of the dblp.xml.
     */
    private class ConfigHandler extends DefaultHandler {

        /** The internal record string builder. */
        private final StringBuilder rec = new StringBuilder();
        /** The current list of collected person names. */
        private final ArrayList<PersonName> names = new ArrayList<>();
        /** The current toc object. */
        private TableOfContents toc;
        /** The current publication stream object. */
        private PublicationStreamTitle stream;
        /** The key of the current record. */
        private String key;
        /** The text content of the current record segment. */
        private String text;
        /** The text content of the current url element. */
        private String url;
        /**
         * The mdate of the current record, coded as an integer. The mdate {@code yyyy-mm-dd}
         * corresponds to integer {@code 10000 * yyyy + 100 * mm + dd}.
         */
        private int mdate;
        /** The year of the current record as an integer. */
        private int year;
        // /** The number of fields within the current record. */
        // private int fields;
        /** The nesting element depth of the current state in an ongoing SAX parsing. */
        private int level = 0;
        /** The number of record elements encountered in an ongoing SAX parsing. */
        private int recordCount = 0;
        /** Signals whether the current record is of tag type 'www'. */
        private boolean isWWW;
        /** Signals whether the current record is of tag type 'person'. */
        private boolean isPersonRecord;
        /** Signals whether the current field element is of type 'author' or 'editor'. */
        private boolean isAuthorOrEditor;
        /** Signals whether the current field element is of type 'journal' or 'booktitle'. */
        private boolean isJournalOrBooktitle;
        /** Signals whether the current field element is of type 'year'. */
        private boolean isYear;
        /** Signals whether the current field element is of type 'url'. */
        private boolean isUrl;
        /** Signals whether the current record contains a 'crossref' tag. */
        private boolean hasCrossref;
        /** The input stream identifying the dblp.dtd file. */
        private final InputStream dtdInputStream;

        /**
         * Creates a new config handler and registers the the dblp.dtd file input stream.
         *
         * @param dtdInputStream The dblp.dtd input.
         */
        public ConfigHandler(InputStream dtdInputStream) {
            this.dtdInputStream = dtdInputStream;
        }

        /**
         * Checks whether the current state of the internal record string builder end with the given
         * suffix String.
         *
         * @param str The suffix to compare to.
         * @return {@code true} if the current record string builder end with {@code str}, otherwise
         *         {@code false}.
         */
        private boolean recEndsWith(String str) {
            int lr = this.rec.length(), ls = str.length();
            if (lr < ls) return false;
            return this.rec.substring(lr - ls).equals(str);
        }

        @Override
        public void startElement(@SuppressWarnings("unused") String namespaceURI,
                @SuppressWarnings("unused") String localName, String rawName, Attributes atts)
                throws SAXException {
            this.level++;
            if (this.level == 2) {
                LOG.trace("+ opening", rawName);
                this.rec.setLength(0);
                this.key = null;
                this.mdate = 0;
                // this.fields = 0;
                this.isPersonRecord = (rawName.equals("person"));
                this.isAuthorOrEditor = false;
                this.isJournalOrBooktitle = false;
                this.isYear = false;
                this.isUrl = false;
                this.hasCrossref = false;
                this.isWWW = (rawName.equals("www"));
                this.names.clear();
                this.toc = null;
                this.stream = null;
                this.year = 0;
            }
            if (this.level == 3) {
                LOG.trace("| field", rawName);
                if (rawName.equals("author")) {
                    this.isAuthorOrEditor = true;
                    rawName = "0";
                    this.text = "";
                }
                else if (rawName.equals("editor")) {
                    this.isAuthorOrEditor = true;
                    rawName = "1";
                    this.text = "";
                }
                else if (rawName.equals("journal")) {
                    this.isJournalOrBooktitle = true;
                    rawName = "2";
                    this.text = "";
                }
                else if (rawName.equals("booktitle")) {
                    this.isJournalOrBooktitle = true;
                    rawName = "3";
                    this.text = "";
                }
                else if (rawName.equals("year")) {
                    this.isYear = true;
                    rawName = "4";
                    this.text = "";
                }
                else if (rawName.equals("url")) {
                    this.isUrl = true;
                    this.url = "";
                }
                else if (rawName.equals("crossref")) {
                    this.hasCrossref = true;
                }
            }
            this.rec.append('<').append(rawName);
            int l = atts.getLength();
            for (int i = 0; i < l; i++) {
                String atName = atts.getLocalName(i);
                if (atName == null || atName.isEmpty()) atName = atts.getQName(i);
                LOG.trace("| attribute", atName + "=" + atts.getValue(i));

                if (this.level == 2 && atName.equals("key")) {
                    this.key = atts.getValue(i);
                    if (this.key.length() > 63) {
                        LOG.warn("too long key", this.key);
                    }
                    continue;
                }
                if (this.level == 2 && atName.equals("mdate")) {
                    String md = atts.getValue(i);
                    if (md != null && md.length() == 10) {
                        char y4 = md.charAt(0), y3 = md.charAt(1), y2 = md.charAt(2),
                                y1 = md.charAt(3), m2 = md.charAt(5), m1 = md.charAt(6),
                                d2 = md.charAt(8), d1 = md.charAt(9);
                        if (Character.isDigit(y4) && Character.isDigit(y3) && Character.isDigit(y2)
                                && Character.isDigit(y1) && Character.isDigit(m2)
                                && Character.isDigit(m1) && Character.isDigit(d2)
                                && Character.isDigit(d1) && md.charAt(4) == '-'
                                && md.charAt(7) == '-') {
                            this.mdate =
                                    ((((((Character.digit(y4, 10) * 10 + Character.digit(y3, 10))
                                            * 10 + Character.digit(y2, 10)) * 10
                                            + Character.digit(y1, 10)) * 10
                                            + Character.digit(m2, 10)) * 10
                                            + Character.digit(m1, 10)) * 10
                                            + Character.digit(d2, 10)) * 10
                                            + Character.digit(d1, 10);
                            continue;
                        }
                    }
                }
                this.rec.append(' ').append(atName).append('=').append('"');
                escapeXmlEntities(atts.getValue(i), this.rec);
                this.rec.append('"');
            }
            if (this.isAuthorOrEditor || this.isJournalOrBooktitle || this.isYear)
                this.rec.append('/');
            this.rec.append('>');
        }

        @Override
        public void endElement(@SuppressWarnings("unused") String namespaceURI,
                @SuppressWarnings("unused") String localName, String rawName)
                throws SAXException {
            this.level--;
            if (this.isAuthorOrEditor) {
                this.isAuthorOrEditor = false;
                PersonName persName = Mmdb.this.persNameMap.get(this.text);
                if (persName == null) {
                    // FIXME: disabled exceptions since SAX parsers are utterly useless pieces of
                    // garbage
                    // try {
                    persName = new PersonName(this.text);
                    // }
                    // catch (InvalidPersonNameException ex) {
                    // // System.err.println( "[WARN] invalid name \"" + this.text + "\": " +
                    // // this.key)
                    // LOG.append("[WARN] encountered invalid name
                    // \"").append(this.text).append("\": "
                    // + this.key).append("\n");
                    // return;
                    // }
                    Mmdb.this.persNameMap.put(this.text, persName);
                }
                this.names.add(persName);
                return;
            }
            if (this.isJournalOrBooktitle) {
                this.isJournalOrBooktitle = false;
                PublicationStreamTitle streamTitle;
                if (rawName.equals("journal")) {
                    streamTitle = Mmdb.this.journalMap.get(this.text);
                    if (streamTitle == null) {
                        JournalTitle jour = new JournalTitle(this.text);
                        Mmdb.this.journalMap.put(this.text, jour);
                        streamTitle = jour;
                    }
                }
                else {
                    streamTitle = Mmdb.this.booktitleMap.get(this.text);
                    if (streamTitle == null) {
                        BookTitle book = new BookTitle(this.text);
                        Mmdb.this.booktitleMap.put(this.text, book);
                        streamTitle = book;
                    }
                }
                if (this.stream != null) {
                    LOG.warn("more than one journal/booktitle", this.key);
                    if (!(this.stream instanceof MultiStreamTitle)) {
                        MultiStreamTitle multiTitle = new MultiStreamTitle(this.stream);
                        this.stream = multiTitle;
                    }
                    ((MultiStreamTitle) this.stream).add(streamTitle);
                }
                else {
                    this.stream = streamTitle;
                }
                return;
            }
            if (this.isYear) {
                this.isYear = false;
                if (this.text.length() != 4 || !Character.isDigit(this.text.charAt(0))
                        || !Character.isDigit(this.text.charAt(1))
                        || !Character.isDigit(this.text.charAt(2))
                        || !Character.isDigit(this.text.charAt(3))) {
                    LOG.warn("illegal year string", this.key);
                    return;
                }
                try {
                    this.year = Integer.parseInt(this.text);
                }
                catch (NumberFormatException e) {
                    LOG.warn("cannot parse year as integer", this.key);
                    return;
                }
                return;
            }
            if (this.level == 2) {
                if (this.isUrl && !this.isPersonRecord && this.url.startsWith("db/")) {
                    this.isUrl = false;
                    int pos = this.url.lastIndexOf('.');
                    if (pos < 0) pos = this.url.lastIndexOf('#');
                    if (pos > -1) {
                        String tocKey = this.url.substring(0, pos) + ".bht";
                        this.toc = Mmdb.this.tocMap.get(tocKey);
                        if (this.toc == null) {
                            this.toc = new TableOfContents(tocKey);
                            Mmdb.this.tocMap.put(tocKey, this.toc);
                        }
                    }
                }
                if (this.isWWW && rawName.equals("title") && recEndsWith("<title>Home Page")) {
                    this.isPersonRecord = true;
                    this.rec.setLength(this.rec.length() - 16);
                    return;
                }
            }

            this.rec.append("</").append(rawName).append('>');

            // end of record
            if (this.level == 1) {
                LOG.trace("- closing", rawName);
                this.recordCount++;
                if (this.recordCount % 100000 == 0) {
                    LOG.info("parsing dblp XML", this.recordCount + " records ...");
                }
                if (this.key == null) {
                    LOG.error("record without key", "skipping record:\n" + this.rec.toString());
                    return;
                }
                if (this.mdate == 0) {
                    LOG.warn("mdate missing", this.key);
                }
                PersonName[] personNames = EMPTY_PERSON_NAME_ARRAY;
                int numOfNames = this.names.size();
                if (numOfNames > 0) personNames = this.names.toArray(new PersonName[numOfNames]);
                if (this.isPersonRecord && !this.hasCrossref) {
                    byte[] byteArray = null;
                    int len = this.rec.length();
                    if (this.isWWW) {
                        if (len != 15) {
                            this.rec.replace(len - 4, len - 1, "person");
                            this.rec.replace(1, 4, "person");
                            byteArray = this.rec.toString().getBytes(UTF8);
                        }
                    }
                    else {
                        if (len != 21) byteArray = this.rec.toString().getBytes(UTF8);
                    }
                    if (this.stream != null) {
                        LOG.warn("journal/booktitle in person record", this.key);
                    }
                    if (this.year != 0) {
                        LOG.warn("year in person record", this.key);
                    }
                    try {
                        Person pers =
                                new Person(Mmdb.this, this.key, this.mdate, byteArray, personNames);
                        Mmdb.this.personMap.put(this.key, pers);
                        for (PersonName name : this.names) {
                            name.setPerson(pers);
                        }
                    }
                    catch (IllegalArgumentException ex) {
                        LOG.warn("missing name(s) in person record", this.key);
                    }
                }
                // else if (this.fields == 1 && this.hasCrossref) {
                else if ((this.isPersonRecord || this.isWWW) && this.hasCrossref) {
                    Redirect redir =
                            new Redirect(Mmdb.this, this.key, this.mdate, this.rec.toString().getBytes(UTF8));
                    Mmdb.this.redirectMap.put(this.key, redir);
                }
                else {
                    Publication publ =
                            new Publication(Mmdb.this, this.key, this.mdate, this.rec.toString().getBytes(UTF8), personNames, this.toc, this.stream, this.year);
                    Mmdb.this.publicationMap.put(this.key, publ);
                    if (this.toc == null) {
                        String tag = publ.getTag();
                        if (tag.equals("inproceedings") || tag.equals("article")
                                || tag.equals("incollection")) {
                            String publKey = this.key;
                            if (!publKey.startsWith("tr/") && !publKey.startsWith("persons/")
                                    && !publKey.startsWith("dblpnote/")) {
                                LOG.warn("url field missing", publKey);
                            }
                        }
                    }
                    else this.toc.add(publ);
                }
            }
            if (this.level == 0) {
                LOG.info("number of records", this.recordCount);
                LOG.info("number of publications", Mmdb.this.publicationMap.size());
                LOG.info("number of persons", Mmdb.this.personMap.size());
                LOG.info("number of redirects", Mmdb.this.redirectMap.size());
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (this.isAuthorOrEditor || this.isJournalOrBooktitle || this.isYear) {
                this.text += new String(ch, start, length);
            }
            else {
                escapeXmlEntities(ch, start, length, this.rec);
            }
            if (this.isUrl) {
                this.url += new String(ch, start, length);
            }
        }

        /**
         * Appends the given char sequence to the given string builder while escaping all
         * occurrences of {@code <, >, &, ", '} by using their named XML entities.
         * <p>
         * Note that for some obscure reasons and other than
         * {@link #escapeXmlEntities(char[], int, int, StringBuilder)} this method <em>does</em>
         * also escape {@code "} and {@code '}.
         *
         * @param in The input char sequence.
         * @param out The output string builder.
         */
        private void escapeXmlEntities(CharSequence in, StringBuilder out) {
            int l = in.length();
            for (int i = 0; i < l; i++) {
                char c = in.charAt(i);
                switch (c) {
                    case '<':
                        out.append("&lt;");
                        break;
                    case '>':
                        out.append("&gt;");
                        break;
                    case '&':
                        out.append("&amp;");
                        break;
                    case '"':
                        out.append("&quot;");
                        break;
                    case '\'':
                        out.append("&apos;");
                        break;
                    default:
                        out.append(c);
                }
            }
        }

        /**
         * Appends the given subsequence of a char array to the given string builder while escaping
         * all occurrences of {@code <, >, &} by using their named XML entities.
         * <p>
         * Note that for some obscure reasons and other than
         * {@link #escapeXmlEntities(CharSequence, StringBuilder)} this method <em>does not</em>
         * escape {@code "} and {@code '}.
         *
         * @param ch The input char array.
         * @param start The starting index in the char array (inclusive).
         * @param length the number of characters to append, beginning from {@code start}.
         * @param out The output string builder.
         */
        private void escapeXmlEntities(char[] ch, int start, int length, StringBuilder out) {
            while (length > 0) {
                char c = ch[start++];
                length--;
                switch (c) {
                    case '<':
                        out.append("&lt;");
                        break;
                    case '>':
                        out.append("&gt;");
                        break;
                    case '&':
                        out.append("&amp;");
                        break;
                    // case '"': out.append("&quot;"); break;
                    default:
                        out.append(c);
                }
            }
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {

            StringWriter str = new StringWriter();
            exception.printStackTrace(new PrintWriter(str));
            LOG.warn("parser warning", str.toString());
            throw new SAXException("parser warning", exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {

            StringWriter str = new StringWriter();
            exception.printStackTrace(new PrintWriter(str));
            LOG.error("parser error", str.toString());
            throw new SAXException("parser error", exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {

            StringWriter str = new StringWriter();
            exception.printStackTrace(new PrintWriter(str));
            LOG.fatal("parser fatal error", str.toString());
            throw new SAXException("parser fatal error", exception);
        }

        @Override
        public InputSource resolveEntity(@SuppressWarnings("unused") String publicId,
                @SuppressWarnings("unused") String systemId)
                throws IOException, SAXException {

            if (this.dtdInputStream != null) return new InputSource(this.dtdInputStream);
            return null;
        }
    }

    /** The content of an empty dblp.xml. */
    private static final String EMPTY_DBLP_XML = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
            + "<!DOCTYPE dblp PUBLIC \"https://dblp.org/xml/dblp.dtd\" \"\">\n" + "<dblp></dblp>\n";
    /** The person-key-to-person-record map. */
    private final Map<String, Person> personMap;
    /** An minimal DTD for the empty dblp.xml */
    private static final String EMPTY_DTD = "<!ELEMENT dblp EMPTY>";
    /** An empty MMDB. Used mainly for testing code without building an actual Mmdb. */
    public static final MmdbInterface EMTPY_MMDB;
    static {
        try (InputStream xmlBytes = new ByteArrayInputStream(EMPTY_DBLP_XML.getBytes());
                InputStream dtdBytes = new ByteArrayInputStream(EMPTY_DTD.getBytes())) {
            EMTPY_MMDB = new Mmdb(xmlBytes, dtdBytes, false);
        }
        catch (IOException | SAXException ex) {
            // should not happen
            throw new UndeclaredThrowableException(ex);
        }
    }

    /**
     * Constructs the main memory database for the given dblp records from the input stream.
     *
     * @param xmlInputStream The dblp XML records to be loaded into the main memory database.
     * @param dtdInputStream The content of the dblp.dtd. If set to {@code null}, the dblp.dtd file
     *            needs to be available in the classpath.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws IOException if any IO errors occur while parsing the XML stream.
     * @throws SAXException if any SAX errors occur while parsing the XML stream.
     */
    public Mmdb(InputStream xmlInputStream, InputStream dtdInputStream, boolean verbose)
            throws IOException, SAXException {

        if (verbose) LOG.setLevel(MmdbLogLevel.DEBUG);

        // guarantee entityExpansionLimit
        String limit = System.getProperty("entityExpansionLimit");
        if (limit == null || Integer.parseInt(limit) < 2000000)
            System.setProperty("entityExpansionLimit", "2000000");

        // init maps
        this.publicationMap = new TreeMap<>();
        this.personMap = new TreeMap<>();
        this.redirectMap = new TreeMap<>();
        this.persNameMap = new TreeMap<>();
        this.journalMap = new TreeMap<>();
        this.booktitleMap = new TreeMap<>();
        this.tocMap = new TreeMap<>();

        // parse XML
        long start, end;
        start = System.currentTimeMillis();
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            ConfigHandler handler = new ConfigHandler(dtdInputStream);
            parser.getXMLReader().setFeature("http://xml.org/sax/features/validation", true);
            parser.parse(xmlInputStream, handler);
        }
        catch (ParserConfigurationException ex) {
            // should not happen
            throw new UndeclaredThrowableException(ex);
        }
        // check for any unlinked person names
        for (PersonName persName : this.persNameMap.values()) {
            if (persName.getPerson() == null) {
                LOG.error("no person record", persName.name());
            }
        }
        end = System.currentTimeMillis();
        LOG.info("parsing XML time", (end - start) + " ms");

        start = System.currentTimeMillis();
        // count publications for each person
        for (Publication publ : this.publicationMap.values()) {
            for (PersonName persName : publ.getNamesArray()) {
                try {
                    persName.getPerson().incr();
                }
                catch (NullPointerException ex) {
                    continue;
                }
            }
        }
        // link back from person to publication
        for (Publication publ : this.publicationMap.values()) {
            PersonName[] persNames = publ.getNamesArray();
            for (PersonName persName : persNames) {
                try {
                    persName.getPerson().addPublication(publ);
                }
                catch (NullPointerException ex) {
                    continue;
                }
            }
        }
        // check if link back is complete
        for (Person pers : this.personMap.values()) {
            if (pers.count() > 0) {
                LOG.error("missing publication(s)", String.format("%s (%d)", pers.getPrimaryName(), pers.count()));
            }
        }
        end = System.currentTimeMillis();
        LOG.info("graph reversal time", (end - start) + " ms");
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
    public Mmdb(InputStream xmlInputStream, boolean verbose) throws IOException, SAXException {
        this(xmlInputStream, null, verbose);
    }

    /**
     * Constructs the main memory database for the given dblp file.
     *
     * @param xmlFilename The dblp XML file to be loaded into the main memory database, the file may
     *            be compressed (.gz).
     * @param dtdFilename The dblp.dtd file.
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws NullPointerException if a file is given as {@code null}.
     * @throws FileNotFoundException if a given file cannot be opened for reading.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    public Mmdb(String xmlFilename, String dtdFilename, boolean verbose)
            throws NullPointerException, FileNotFoundException, IOException, SAXException {
        this(xmlFilename.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(xmlFilename))
                : new FileInputStream(new File(xmlFilename)), new FileInputStream(new File(dtdFilename)), verbose);
    }

    /**
     * Constructs the main memory database for the given dblp file. The dblp.dtd file needs to be
     * available in the classpath.
     *
     * @param xmlFilename The dblp XML file to be loaded into the main memory database, the file may
     *            be compressed (.gz).
     * @param verbose If {@code true}, progress and timing information is logged to {@code stderr}.
     * @throws NullPointerException if {@code xmlFilename == null}.
     * @throws FileNotFoundException if {@code xmlFilename} cannot be opened for reading.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    public Mmdb(String xmlFilename, boolean verbose)
            throws NullPointerException, FileNotFoundException, IOException, SAXException {
        this(xmlFilename.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(xmlFilename))
                : new FileInputStream(new File(xmlFilename)), null, verbose);
    }

    @Override
    public Publication getPublication(String key) throws NullPointerException {
        return this.publicationMap.get(key);
    }

    @Override
    public Collection<Publication> getPublications() {
        return Collections.unmodifiableCollection(this.publicationMap.values());
    }

    @Override
    public Stream<Publication> publications() {
        return this.publicationMap.values().stream();
    }

    @Override
    public int numberOfPublications() {
        return this.publicationMap.size();
    }

    @Override
    public Person getPerson(String key) throws NullPointerException {
        return this.personMap.get(key);
    }

    @Override
    public Collection<Person> getPersons() {
        return Collections.unmodifiableCollection(this.personMap.values());
    }

    @Override
    public Stream<Person> persons() {
        return this.personMap.values().stream();
    }

    @Override
    public int numberOfPersons() {
        return this.personMap.size();
    }

    @Override
    public Redirect getRedirect(String key) throws NullPointerException {
        return this.redirectMap.get(key);
    }

    @Override
    public Collection<Redirect> getRedirects() {
        return Collections.unmodifiableCollection(this.redirectMap.values());
    }

    @Override
    public Stream<Redirect> redirects() {
        return getRedirects().stream();
    }

    @Override
    public int numberOfRedirects() {
        return this.redirectMap.size();
    }

    @Override
    public PersonName getPersonName(String name) throws NullPointerException {
        return this.persNameMap.get(name);
    }

    @Override
    public Collection<PersonName> getPersonNames() {
        return Collections.unmodifiableCollection(this.persNameMap.values());
    }

    @Override
    public Stream<PersonName> personNames() {
        return this.persNameMap.values().stream();
    }

    @Override
    public int numberOfPersonNames() {
        return this.persNameMap.size();
    }

    @Override
    public TableOfContents getToc(String key) throws NullPointerException {
        return this.tocMap.get(key);
    }

    @Override
    public Collection<TableOfContents> getTocs() {
        return Collections.unmodifiableCollection(this.tocMap.values());
    }

    @Override
    public Stream<TableOfContents> tocs() {
        return this.tocMap.values().stream();
    }

    @Override
    public int numberOfTocs() {
        return this.tocMap.size();
    }

    @Override
    public BookTitle getBookTitle(String title) throws NullPointerException {
        return this.booktitleMap.get(title);
    }

    @Override
    public Collection<BookTitle> getBookTitles() {
        return Collections.unmodifiableCollection(this.booktitleMap.values());
    }

    @Override
    public Stream<BookTitle> bookTitles() {
        return this.booktitleMap.values().stream();
    }

    @Override
    public int numberOfBookTitles() {
        return this.booktitleMap.size();
    }

    @Override
    public JournalTitle getJournal(String title) throws NullPointerException {
        return this.journalMap.get(title);
    }

    @Override
    public Collection<JournalTitle> getJournals() {
        return Collections.unmodifiableCollection(this.journalMap.values());
    }

    @Override
    public Stream<JournalTitle> journals() {
        return this.journalMap.values().stream();
    }

    @Override
    public int numberOfJournals() {
        return this.journalMap.size();
    }

    // TODO: testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) {

        Mmdb.LOG.setLevel(MmdbLogLevel.FATAL);

        if (args.length != 2) {
            System.err.format("USAGE: %s <dblp.xml> <dblp.dtd>\n", Mmdb.class.getSimpleName());
            System.exit(1);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        int numberOfTests = 5;
        double total;
        try {
            System.out.println("just build ...");
            total = 0.0;
            for (int i = 0; i < numberOfTests + 1; i++) {
                double time = testRunJustBuild(dblpXmlFilename, dblpDtdFilename);
                if (i == 0) continue; // warming cache
                System.out.format("#%d:\t%6.2f secs\n", i, time);
                total += time;
            }
            System.out.format("mean:\t%6.2f secs\n", total / numberOfTests);

            System.out.println("build and parse all person names ...");
            total = 0.0;
            for (int i = 0; i < numberOfTests + 1; i++) {
                double time = testRunBuildAndParseAllPersonNames(dblpXmlFilename, dblpDtdFilename);
                if (i == 0) continue; // warming cache
                System.out.format("#%d:\t%6.2f secs\n", i, time);
                total += time;
            }
            System.out.format("mean:\t%6.2f secs\n", total / numberOfTests);
        }
        catch (IOException ex) {
            LOG.fatal("cannot read dblp XML", ex.getMessage());
            return;
        }
        catch (SAXException ex) {
            LOG.fatal("cannot parse XML", ex.getMessage());
            return;
        }
    }

    /**
     * Running time of just building the MMDB.
     *
     * @param dblpXmlFilename The XML file.
     * @param dblpDtdFilename The DTD file.
     * @return The running time in seconds.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     * @throws IOException if any IO errors occur while parsing the XML.
     */
    private static double testRunJustBuild(String dblpXmlFilename, String dblpDtdFilename)
            throws IOException, SAXException {
        long start = System.nanoTime();

        LOG.info("building MMDB", "from " + dblpXmlFilename + " ...");
        Mmdb dblp = new Mmdb(dblpXmlFilename, dblpDtdFilename, false);
        LOG.info("MMDB ready", dblp.numberOfPublications() + " publs, " + dblp.numberOfPersons()
                + " pers");

        double time = (System.nanoTime() - start) / 1000000000.0;
        LOG.info("total running time", String.format("%3.2f secs", time));
        return time;
    }

    /**
     * Running time of building the MMDB and parsing all person names.
     *
     * @param dblpXmlFilename The XML file.
     * @param dblpDtdFilename The DTD file.
     * @return The running time in seconds.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     * @throws IOException if any IO errors occur while parsing the XML.
     */
    private static double testRunBuildAndParseAllPersonNames(String dblpXmlFilename,
            String dblpDtdFilename)
            throws IOException, SAXException {
        long start = System.nanoTime();

        LOG.info("building MMDB", "from " + dblpXmlFilename + " ...");
        Mmdb dblp = new Mmdb(dblpXmlFilename, dblpDtdFilename, false);
        LOG.info("MMDB ready", dblp.numberOfPublications() + " publs, " + dblp.numberOfPersons()
                + " pers");

        for (Person pers : dblp.getPersons()) {
            String idnr = pers.getPrimaryName().idnr();
            if (idnr == null) continue;
        }

        double time = (System.nanoTime() - start) / 1000000000.0;
        LOG.info("total running time", String.format("%3.2f secs", time));
        return time;
    }

}
