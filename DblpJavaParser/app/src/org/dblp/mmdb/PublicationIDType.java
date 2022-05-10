package org.dblp.mmdb;

import java.util.Formatter;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An enumeration of publication IDs.
 */
public enum PublicationIDType implements UrlTypeTagInterface {

    /** A dblp key. */
    DBLP("dblp record key", "https?://dblp\\.org/rec/(?<id>.*)", "https://dblp.org/rec/%s",
            id -> id),
    /** A DNB record ID. */
    DNB("DNB ID", "https?://d-nb\\.info/(?!gnd/)(?<id>.*)", "http://d-nb.info/%s",
            id -> id.toUpperCase().replaceAll("[^0-9X]", "")),
    /** A DOI. */
    DOI("DOI", "https?://((dx\\.)?doi|doi\\.ieeecomputersociety|doi\\.acm)\\.org/(?<id>.*)",
            "https://doi.org/%s", id -> id.toUpperCase()),
    /** A Handle System ID. */
    HANDLE("Handle System ID", "https?://hdl\\.handle\\.net/(?<id>.*)", "http://hdl.handle.net/%s",
            id -> id),
    /** An ISBN. */
    ISBN("ISBN", "https?://www\\.worldcat\\.org/isbn/(?<id>.*)", "https://www.worldcat.org/isbn/%s",
            id -> id.toUpperCase().replaceAll("[^0-9X]", "")),
    /** A URN of the German National Library. */
    URN_NBN_DE("German National Library URN", "https?://nbn-resolving\\.(de|org)/(?<id>urn:.*)",
            "http://nbn-resolving.de/%s", id -> id),

    /** Wikidata entity id for a publication */
    WIKIDATA("wikidata", "https://www.wikidata.org/entity/(?<id>.*)",
            "https://www.wikidata.org/entity/%s", id -> id),

    /** An arXiv identifier. */
    ARXIV("arXiv", "https?://arxiv\\.org/abs/(?<id>.*)", "https://arxiv.org/abs/%s", id -> id);

    /** The descriptive label of this ID type. */
    private String label;
    /** The regex pattern to identify this ID from an URL. */
    private Pattern pattern;
    /** The format to build the URL from the ID. */
    private String format;
    /** The normalization function to remove ambiguity from an ID. */
    private Function<String, String> normal;

    /**
     * Create a new publication ID.
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
    private PublicationIDType(String label, String pattern, String format,
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
    public static PublicationIDType of(String url) {
        for (PublicationIDType type : PublicationIDType.values()) {
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

    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.format("USAGE: %s <dblp.xml> <dblp.dtd>\n", RecordDb.class.getSimpleName());
            System.exit(1);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        RecordDbInterface dblp;
        if (dblpXmlFilename.toLowerCase().endsWith(".gz")) {
            dblp = RecordDb.fromGzip(dblpXmlFilename, dblpDtdFilename, true);
        }
        else dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, true);

        for (PublicationIDType type : PublicationIDType.values()) {
            int num = dblp.numberOfPublications(type);
            if (num == 0) continue;
            System.out.println("number of " + type.label() + "s: " + num);
        }

    }
}
