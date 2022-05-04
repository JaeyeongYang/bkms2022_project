package org.dblp.mmdb;

import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An enumeration of publication stream IDs.
 */
public enum PublicationStreamIDType {

    /** A dblp key. */
    DBLP("dblp stream key", "https?://dblp\\.org/db/(?<id>.*)", "https://dblp.org/db/%s"),
    /** An ISSN. */
    ISSN("ISSN", "https?://www\\.worldcat\\.org/issn/(?<id>.*)",
            "https://www.worldcat.org/issn/%s"),
    /** An ISSN. */
    SPRINGER_LOD("Springer LOD ID", "https?://lod\\.springer\\.com/data/(?<id>.*)",
            "http://lod.springer.com/data/%s"),
    /** A WikiData identifier. */
    WIKIDATA("WikiData entity ID", "https?://(www\\.)?wikidata\\.org/(wiki|entity)/(?<id>.*)",
            "https://www.wikidata.org/entity/%s");

    /** The descriptive label of this ID type. */
    private String label;
    /** The regex pattern to identify this ID from an URL. */
    private Pattern pattern;
    /** The format to build the URL from the ID. */
    private String format;

    /**
     * Create a new stream ID.
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
     */
    private PublicationStreamIDType(String label, String pattern, String format) {
        this.label = label;
        this.pattern = Pattern.compile(pattern);
        this.format = format;
    }

    /**
     * Guess the ID type from the given URL.
     *
     * @param url The URL to check.
     * @return The type, or {@code null} if no matching type can be identified.
     */
    public static PublicationStreamIDType of(String url) {
        for (PublicationStreamIDType type : PublicationStreamIDType.values()) {
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
     * Extract the ID from the given URL.
     *
     * @param url The URL.
     * @return The ID, or {@code null} if no matching ID can be found.
     */
    public String getID(String url) {
        Matcher matcher = this.pattern.matcher(url);
        if (matcher.matches()) return matcher.group("id");
        return null;
    }

    /**
     * Build and retrieve the resource URI of the given ID.
     *
     * @param id The ID.
     * @return The resource URI.
     */
    public String getResourceUri(String id) {
        return String.format(this.format, id);
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

    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.format("USAGE: %s <dblp.xml> <dblp.dtd>\n", PublicationIDType.class.getSimpleName());
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
