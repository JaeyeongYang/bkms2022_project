package org.dblp.mmdb;

/**
 * The parsing of a person name in dblp. Note that this class does not necessarily classify first
 * names, last names, etc. correctly, but according to the conventions in dblp.
 *
 * @author Marcel R. Ackermann
 * @version 2017-02-14
 */
class PersonNameParsing {

    /** The first name part of a dblp person name */
    final String first;
    /** The last name part of a dblp person name */
    final String last;
    /** The name suffix part of a dblp person name */
    final String suffix;
    /** The homonym id part of a dblp person name, or <tt>null</tt> if no such number is given. */
    final String idnr;
    /** The dblp URL part that belongs to this person name. */
    final String urlpt;

    /** The entity name map for all Latin 1 characters from code point 192 to 255. */
    static private final String entityMap[] = { "Agrave", "Aacute", "Acirc", "Atilde", "Auml",
            "Aring", "AElig", "Ccedil", "Egrave", "Eacute", "Ecirc", "Euml", "Igrave", "Iacute",
            "Icirc", "Iuml", "ETH", "Ntilde", "Ograve", "Oacute", "Ocirc", "Otilde", "Ouml",
            "times", "Oslash", "Ugrave", "Uacute", "Ucirc", "Uuml", "Yacute", "THORN", "szlig",
            "agrave", "aacute", "acirc", "atilde", "auml", "aring", "aelig", "ccedil", "egrave",
            "eacute", "ecirc", "euml", "igrave", "iacute", "icirc", "iuml", "eth", "ntilde",
            "ograve", "oacute", "ocirc", "otilde", "ouml", "247", "oslash", "ugrave", "uacute",
            "ucirc", "uuml", "yacute", "thorn", "yuml" };

    /**
     * Maps a given string with non-ASCII characters to its dblp URL representation.
     *
     * @param str The input string.
     * @return The mapped input.
     */
    private static String mapUrl(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < str.length(); pos++) {
            char ch = str.charAt(pos);
            if (ch == ' ') sb.append('_');
            else if (ch > 191 && ch < 256) sb.append('=').append(entityMap[ch - 192]).append('=');
            // FIXME: this will not work for surrogate pair characters
            else if (ch > 255) sb.append('=').append((int) ch).append('=');
            else if (Character.isLetterOrDigit(ch)) sb.append(ch);
            else sb.append('=');
        }
        return sb.toString();
    }

    /**
     * Creates a new person name parsing.
     *
     * @param name The person name. Note that the name may neither be empty nor consist solely of a
     *            recognized name suffix.
     * @throws NullPointerException if {@code name == null}.
     * @throws InvalidPersonNameException if an irregular name has been given.
     */
    PersonNameParsing(String name) throws NullPointerException, InvalidPersonNameException {

        String tmpFirst = null;
        String tmpLast = null;
        String tmpSuffix = null;
        String tmpIdnr = null;
        String tmpUrlpt = null;

        // kill whitey
        name = name.trim();
        name = name.replaceAll("\\s+", " ");

        // sanity check
        // FIXME: disabled exceptions since SAX parsers are utterly useless pieces of garbage
        if (name.length() < 1) {
            this.first = null;
            this.last = "";
            this.suffix = null;
            this.idnr = null;
            this.urlpt = "/:";
            return;
            // throw new InvalidPersonNameException("cannot parse empty name");
        }

        // find homonym id
        if (name.matches(".* \\d{4}")) {
            tmpIdnr = name.substring(name.length() - 4);
            name = name.substring(0, name.length() - 5);
        }

        // find first name, last name, and maybe suffix
        int index = name.lastIndexOf(" ");
        if (index > -1) {
            tmpFirst = name.substring(0, index);
            tmpLast = name.substring(index + 1);
        }
        else {
            tmpFirst = "";
            tmpLast = name;
        }
        if (tmpLast.matches("Jr\\.|II|III|IV")) {
            tmpSuffix = tmpLast;
            index = tmpFirst.lastIndexOf(" ");
            if (index > -1) {
                tmpLast = tmpFirst.substring(index + 1);
                tmpFirst = tmpFirst.substring(0, index);
            }
            else {
                tmpLast = tmpFirst;
                tmpFirst = "";
            }
        }

        // map special characters
        String mappedFirst = mapUrl(tmpFirst);
        String mappedLast = mapUrl(tmpLast);
        String mappedSuffix = mapUrl(tmpSuffix);

        // sanity check
        // FIXME: disabled exceptions since SAX parsers are utterly useless pieces of garbage
        if (mappedLast == null || mappedLast.length() < 1) {
            this.first = null;
            this.last = tmpSuffix != null ? tmpSuffix : "";
            this.suffix = null;
            this.idnr = null;
            this.urlpt = tmpSuffix != null
                    ? mappedSuffix.substring(0, 1).toLowerCase() + "/" + mappedSuffix + ":"
                    : "/:";
            return;
            // throw new InvalidPersonNameException("irregular name '" + name + "', cannot parse");
        }

        // build urlpt
        String initial = mappedLast.substring(0, 1).toLowerCase();
        if (!Character.isLetter(initial.charAt(0))) initial = "=";
        tmpUrlpt = initial + "/" + mappedLast;
        if (tmpSuffix != null) tmpUrlpt += "_" + mappedSuffix;
        if (tmpIdnr != null) tmpUrlpt += "_" + tmpIdnr;
        tmpUrlpt += ":" + mappedFirst;
        if (tmpFirst.equals("")) tmpFirst = null;

        if (tmpFirst != null) this.first = tmpFirst.intern();
        else this.first = null;
        if (tmpLast != null) this.last = tmpLast.intern();
        else this.last = null;
        if (tmpSuffix != null) this.suffix = tmpSuffix.intern();
        else this.suffix = null;
        if (tmpIdnr != null) this.idnr = tmpIdnr.intern();
        else this.idnr = null;
        this.urlpt = tmpUrlpt.intern();
    }

    /**
     * Returns a string representation of the parsed name.
     *
     * @param withHomonymId If true, the homonym id number will be given, otherwise it is dropped.
     * @return The name string.
     */
    String name(boolean withHomonymId) {

        StringBuilder result = new StringBuilder();

        if (this.first != null) result.append(this.first).append(" ");
        if (this.last != null) result.append(this.last).append(" ");
        if (this.suffix != null) result.append(this.suffix).append(" ");
        if (withHomonymId && this.idnr != null) result.append(this.idnr);

        return result.toString().trim();
    }

    /**
     * Returns a string representation of the parsed name, including any possible homonym id number.
     *
     * @return The name string.
     */
    String name() {
        return this.name(true);
    }

    /**
     * Returns a reduced string representation of the parsed name with all abbreviated name parts,
     * suffices, homonym numbers, etc stripped away.
     * <p>
     * E.g.:
     * <ul>
     * <li>{@code "Wei Wang 0017"} will be returned as {@code "Wei Wang"}
     * <li>{@code "Alfred O. Hero III"} will be returned as {@code "Alfred Hero"}
     * </ul>
     *
     * @return A reduced string representation of this ParsedPersonName.
     */
    String coreName() {

        if (this.first == null) return this.last;

        String[] firstNameParts = this.first.split(" ");
        StringBuilder result = new StringBuilder();

        for (String firstNamePart : firstNameParts) {
            if (firstNamePart.length() < 2) continue;
            if (firstNamePart.endsWith(".")) continue;
            if (firstNamePart.equals("Jr.") || firstNamePart.equals("Sr.")
                    || firstNamePart.equals("II") || firstNamePart.equals("III")
                    || firstNamePart.equals("IV"))
                continue;
            if (firstNamePart.matches("[0-9]+")) continue;
            result.append(firstNamePart).append(" ");
        }
        result.append(this.last);

        return result.toString();
    }

    /**
     * Returns the CompleteSearch facet identifier of the parsed name.
     *
     * @return The CS facet ID.
     */
    String csFacetId() {

        return this.name().replaceAll(" ", "_");
    }

    // TODO: Testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) {

        for (String arg : args) {
            try {
                PersonNameParsing name = new PersonNameParsing(arg);
                System.out.println(name);
                System.out.println(" firstname: " + name.first);
                System.out.println(" lastname:  " + name.last);
                System.out.println(" suffix:    " + name.suffix);
                System.out.println(" idnr:      " + name.idnr);
                System.out.println(" urlpt:     " + name.urlpt);
                System.out.println(" name:      " + name.name());
                System.out.println(" core name: " + name.coreName());
                System.out.println(" CS facet:  " + name.csFacetId());
                System.out.println();
            }
            catch (NullPointerException | InvalidPersonNameException ex) {
                ex.printStackTrace();
            }
        }
    }

}
