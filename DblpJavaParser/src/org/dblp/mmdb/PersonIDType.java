package org.dblp.mmdb;

import java.util.Formatter;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An enumeration of person IDs.
 */
public enum PersonIDType implements UrlTypeTagInterface {

    /** An ACM author ID. */
    ACM_DL("ACM author ID", "https?://dl\\.acm\\.org/author_page.cfm\\?id=(?<id>.*)",
            "https://dl.acm.org/author_page.cfm?id=%s", id -> id.replaceAll("[^0-9]", "")),
    /** An arXiv author ID. */
    ARXIV("arXiv author ID", "https?://arxiv\\.org/a/(?<id>.*)", "https://arxiv.org/a/%s",
            id -> id),
    /** A subject issued by Bibliotheque nationale de France. */
    BNF("BNF Archival Resource Key", "http://catalogue.bnf.fr/ark:/12148/cb(?<id>.*)",
            "https://catalogue.bnf.fr/ark:/12148/cb%s", id -> id),
    /** A CNPq Lattes ID. */
    CNPQ_LATTES("CNPq Lattes ID", "https?://lattes\\.cnpq\\.br/(?<id>.*)",
            "http://lattes.cnpq.br/%s", id -> id),
    /** A Colciencias ScienTI ID. */
    COLCIENCIAS_SCIENTI("Colciencias ScienTI ID",
            "https?://scienti[0-9]?\\.colciencias\\.gov\\.co(:8081)?/cvlac/visualizador/generarCurriculoCv.do?cod_rh=(?<id>.*)",
            "https://scienti.colciencias.gov.co/cvlac/visualizador/generarCurriculoCv.do?cod_rh=%s",
            id -> id.replaceAll("[^0-9]", "")),
    /** A dblp PID. */
    DBLP("dblp author PID", "https?://dblp\\.org/pid/(?<id>.*)", "https://dblp.org/pid/%s",
            id -> id),
    /** A DFG GEPRIS ID. */
    DFG_GEPRIS("DFG GEPRIS ID", "https?://gepris\\.dfg\\.de/gepris/person/(?<id>.*)",
            "https://gepris.dfg.de/gepris/person/%s", id -> id),
    /** A WorldCat's FAST Linked Data authority file ID */
    FAST("OCLC FAST Linked Data ID", "https?://(id|experimental).worldcat.org/fast/(?<id>.*)",
            "https://id.worldcat.org/fast/%s", id -> id),
    /** A GitHub user identifier. */
    GITHUB("GitHub user ID", "https?://(www\\.)?github\\.com/(?<id>.*)", "https://github.com/%s",
            id -> id),
    /** A DNB GND ID. */
    GND("DNB GND ID", "https?://d-nb\\.info/gnd/(?<id>.*)", "https://d-nb.info/gnd/%s",
            id -> id.toUpperCase().replaceAll("[^0-9X]", "")),
    /** A Google Scholar identifier. */
    GOOGLE_SCHOLAR("Google Scholar author ID",
            "https?://scholar\\.google\\.[a-z]+(\\.[a-z]+)?/citations\\?user=(?<id>.*)",
            "https://scholar.google.com/citations?user=%s",
            id -> id.replaceAll("[^0-9a-zA-Z_-]", "")),
    /** An IMDB ID. */
    IMDB("IMDB", "https?://(www\\.)?imdb\\.com/name/(?<id>.*)/?", "https://www.imdb.com/name/%s/",
            id -> id.toLowerCase().replaceAll("[^nm0-9]", "")),
    /** An ISNI. */
    ISNI("ISNI", "https?://(www\\.)?isni\\.org/isni/(?<id>.*)", "http://isni.org/isni/%s",
            id -> id.toUpperCase().replaceAll("[^0-9X]", "")),
    /** A LinkedIn URL. */
    // FIXME: cheap workaround for LinkedIn, whole URL as ID
    LINKEDIN("LinkedIn URL", "https?://(?<id>([a-z]+\\.)?linkedin.com/.*)", "https://%s", id -> id),
    /** A Library of Congress ID. */
    LOC("Library of Congress ID", "https?://id\\.loc\\.gov/authorities/names/(?<id>.*?)(\\.html)?",
            "https://id.loc.gov/authorities/names/%s", id -> id),
    /** An author ID at MathSciNet. */
    MATHSCINET("MathSciNet author ID",
            "https?://(www\\.)?ams\\.org/mathscinet/(MRAuthorID/|search/author\\.html\\?mrauthid=)(?<id>.*)",
            "https://www.ams.org/mathscinet/MRAuthorID/%s", id -> id),
    /** An author ID at the Math Genealogy Project. */
    MATH_GENEALOGY("MGP author ID",
            "https?://(www\\.)?genealogy\\.ams\\.org/id\\.php\\?id=(?<id>.*)",
            "http://www.genealogy.ams.org/id.php?id=%s", id -> id),
    /** A Mendeley profile identifier. */
    MENDELEY("Mendeley profile ID", "https?://(www\\.)?mendeley.com/profiles/(?<id>.*)",
            "https://www.mendeley.com/profiles/%s", id -> id),
    /** Authority control ID by the National Diet Library of Japan. */
    NDL("National Diet Library of Japan ID", "https?://id.ndl.go.jp/auth/ndlna/(?<id>.*)",
            "https://id.ndl.go.jp/auth/ndlna/%s", id -> id),
    /** An ORCID. */
    ORCID("ORCID", "https?://(www\\.)?orcid\\.org/(?<id>.*)", "https://orcid.org/%s",
            id -> normalizeOrcid(id)),
    /** An Open Library ID */
    OPEN_LIBRARY("Open Library ID", "https?://openlibrary.org/authors/(?<id>.*)",
            "https://openlibrary.org/authors/%s", id -> id),
    /** A ZPID PsychAuthors ID. */
    PSYCH_AUTHORS("PsychAuthors ID",
            "https?://(www\\.)?(psychauthors|zpid)\\.de/psychauthors/index\\.php\\?wahl=forschung&uwahl=psychauthors&uuwahl=(?<id>.*)",
            "https://www.psychauthors.de/psychauthors/index.php?wahl=forschung&amp;uwahl=psychauthors&amp;uuwahl=%s",
            id -> id),
    /** A Publons ID. */
    PUBLONS("Publons ID", "https?://(www\\.)?publons\\.com/researcher/(?<id>[0-9]*)(/.*)?",
            "https://publons.com/researcher/%s", id -> id),
    /** RePEc short id. */
    REPEC_SHORT_ID("RePEc short ID", "https?://authors.repec.org/pro/(?<id>.*)",
            "https://authors.repec.org/pro/%s", id -> id),
    /** A ResearcherID. */
    RESEARCHER_ID("ResearcherID", "https?://(www\\.)?researcherid\\.com/rid/(?<id>.*)",
            "https://www.researcherid.com/rid/%s", id -> id),
    /** A ResearchGate author ID. */
    RESEARCHGATE("ResearchGate author ID", "https?://(www\\.)?researchgate\\.net/profile/(?<id>.*)",
            "https://www.researchgate.net/profile/%s", id -> id),
    /** A ResearcherID. */
    SCOPUS("Scopus author ID",
            "https?://(www\\.)?scopus\\.com/authid/detail\\.uri\\?authorId=(?<id>.*)",
            "https://www.scopus.com/authid/detail.uri?authorId=%s", id -> id),
    /** Reference authority control number of the French academic libraries. */
    SUDOC("SUDOC identifier", "https?://www\\.idref\\.fr/(?<id>.*)", "https://www.idref.fr/%s",
            id -> id),
    /** A Twitter account ID. */
    TWITTER("Twitter account ID", "https?://(www\\.)?twitter\\.com/(?<id>.*)",
            "https://twitter.com/%s", id -> id.toLowerCase()),
    /** An VIAF ID. */
    VIAF("VIAF ID", "https?://(www\\.)?viaf\\.org/viaf/(?<id>.*)", "https://viaf.org/viaf/%s",
            id -> id.replaceAll("[^0-9]", "")),
    /** A WikiData identifier. */
    WIKIDATA("WikiData entity ID", "https?://(www\\.)?wikidata\\.org/(wiki|entity)/(?<id>.*)",
            "https://www.wikidata.org/entity/%s", id -> id.toUpperCase().replaceAll("[^Q0-9]", "")),
    /** An article ID from English Wikipedia. */
    WIKIPEDIA_EN("English Wikipedia page ID", "https?://en\\.wikipedia\\.org/wiki/(?<id>.*)",
            "https://en.wikipedia.org/wiki/%s", id -> id),
    /** A WikiData identifier. */
    ZBMATH("zbMATH author ID", "https?://(www\\.)?zbmath\\.org/authors/\\?q=ai:(?<id>.*)",
            "https://zbmath.org/authors/?q=ai:%s", id -> id);

    /** The descriptive label of this ID type. */
    private String label;
    /** The regex pattern to identify this ID from an URL. */
    private Pattern pattern;
    /** The format to build the URL from the ID. */
    private String format;
    /** The normalization function to remove ambiguity from an ID. */
    private Function<String, String> normal;

    /**
     * Create a new person ID.
     * <p>
     * The general contract of this enumeration requires that the resource URIs produced by method
     * {@link #getResourceUri(String)} using parameter {@code format} are recognized by method
     * {@link #of(String)} and parameter {@code pattern}.
     *
     * @param label A descriptive label of this ID type.
     * @param pattern The regex pattern to uniquely identify an ID from a URL. The ID part needs to
     *            be marked as a named capture group {@code "id"}, e.g.:
     *            <pre>"https?://(www\\.)?example\\.org/id/(?&lt;id&gt;.*)"}</pre>
     * @param format The format to build the ID URL. This argument needs to be given according to
     *            {@link Formatter} syntax, expecting exactly one {@link String} argument (the ID).
     * @param normal The ID normalization function.
     */
    private PersonIDType(String label, String pattern, String format,
            Function<String, String> normal) {
        this.label = label;
        this.pattern = Pattern.compile(pattern);
        this.format = format;
        this.normal = normal;
    }

    /**
     * Guess the ID type from the given URL.
     *
     * @param url The URL to check.
     * @return The type, or {@code null} if no matching type can be identified.
     */
    public static PersonIDType of(String url) {
        for (PersonIDType type : PersonIDType.values()) {
            Matcher matcher = type.pattern.matcher(url);
            if (matcher.matches()) return type;
        }
        return null;
    }

    /**
     * Retrieve the printable label of this ID type.
     *
     * @return The label.
     */
    public String label() {
        return this.label;
    }

    /**
     * Extract (and normalize) the ID from the given URL.
     *
     * @param url The URL.
     * @return The normalized ID, or {@code null} if no matching ID can be found.
     */
    public String getID(String url) {
        Matcher matcher = this.pattern.matcher(url);
        if (matcher.matches()) return this.normalize(matcher.group("id"));
        return null;
    }

    /**
     * Build and retrieve the resource URI of the given ID.
     *
     * @param id The ID.
     * @return The resource URI.
     */
    public String getResourceUri(String id) {
        return String.format(this.format, this.normalize(id));
    }

    /**
     * Checks whether the given URL matches this IDs pattern.
     *
     * @param url The URL.
     * @return {@code true} if the URL matches, otherwise {@code false}.
     */
    public boolean matchesUrl(String url) {
        return this.pattern.matcher(url).matches();
    }

    /**
     * Transforms the given ID into its normal form.
     *
     * @param id The ID.
     * @return The normalized ID.
     */
    public String normalize(String id) {
        return this.normal.apply(id);
    }

    /**
     * Normalize an ORCID.
     *
     * @param id The id.
     * @return the normalized ID.
     */
    private static String normalizeOrcid(String id) {
        id = id.toUpperCase().replaceAll("[^0-9X]", "");
        String[] parts = id.split("(?<=\\G.{4})");
        StringJoiner joiner = new StringJoiner("-");
        for (String part : parts) {
            joiner.add(part);
        }
        return joiner.toString();
    }

    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.format("USAGE: %s <dblp.xml> <dblp.dtd>\n", PersonIDType.class.getSimpleName());
            System.exit(1);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        RecordDbInterface dblp;
        if (dblpXmlFilename.toLowerCase().endsWith(".gz")) {
            dblp = RecordDb.fromGzip(dblpXmlFilename, dblpDtdFilename, true);
        }
        else dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, true);

        for (PersonIDType type : PersonIDType.values()) {
            int num = dblp.numberOfPersons(type);
            if (num == 0) continue;
            System.out.println("number of " + type.label() + "s: " + num);
        }
    }
}
