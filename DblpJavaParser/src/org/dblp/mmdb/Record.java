package org.dblp.mmdb;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;


/**
 * An abstract base class for dblp records. This class implements several default implementations
 * for storing records as and retrieving data from a space-efficient representation of a dblp
 * record.
 */
public abstract class Record {

    /** The MMDB. */
    protected MmdbInterface dblp;
    /** The key of this record. */
    protected String key;
    /**
     * Integer representation of an mdate. String <code>"yyyy-mm-dd"</code> is stored as integer
     * <pre>mdate = yyyy * 10000 + mm * 100 + dd</pre>.
     */
    protected int mdate;
    /** The formatted mdate string, initialized lazy from {@link #mdate}. */
    protected String mdateString;
    /** The dblp record as space-efficient compressed XML byte array. */
    protected byte[] xml;
    /** The person names referenced in this record. */
    protected PersonName[] names;
    /** The UTF-8 charset constant. */
    static final protected Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The number of PersonNames stored in this record.
     *
     * @return The number of PersonNames.
     */
    public int numberOfNames() {
        if (this.names == null) return 0;
        return this.names.length;
    }

    /**
     * Retrieves the PersonName at the specified index. An index ranges from <code>0</code> to
     * <code>getNumberOfNames() - 1</code>.
     *
     * @param index the index of the PersonName.
     * @return The PersonName at the specified index of this record.
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >=
     *              getNumberOfNames())}
     */
    public PersonName nameAt(int index) {
        if (this.names == null) return null;
        if (index < 0 || index >= this.names.length) throw new IndexOutOfBoundsException();
        return this.names[index];
    }

    /**
     * Returns the index of the specified name within this record's the list of referenced person
     * names. An index ranges from <code>0</code> to <code>getNumberOfNames() - 1</code>. If no such
     * value exists, then {@code -1} is returned.
     *
     * @param name The person name.
     * @return The index of the first occurrence of the specified name, or {@code -1} if there is no
     *         such occurrence.
     */
    public int indexOf(PersonName name) {
        if (this.names == null) return -1;
        int pos = 0;
        for (PersonName author : this.names) {
            if (name.isAliasOf(author)) return pos;
            pos++;
        }
        return -1;
    }

    /**
     * Retrieve the internal array of the person names in this record.
     *
     * @return The person names.
     */
    PersonName[] getNamesArray() {
        return this.names;
    }

    /**
     * Retrieves an unmodifiable List view of the person names in this record.
     *
     * @return The person names as unmodifiable List.
     */
    public List<PersonName> getNames() {
        if (this.names == null) return null;
        return Collections.unmodifiableList(Arrays.asList(this.names));
    }

    /**
     * Returns a sequential stream with the person names in this record as its source.
     *
     * @return The stream of person names.
     */
    public Stream<PersonName> names() {
        return Arrays.stream(this.names);
    }

    /**
     * Retrieve the internal byte array of the XML of this record.
     *
     * @return The XML byte array.
     */
    byte[] getXmlBytes() {
        return this.xml;
    }

    /**
     * Retrieves the tag name starting at the given position in the XML byte array.
     *
     * @param pos The starting position of the first character of the tag name.
     * @return The tag name.
     */
    String getTag(int pos) {
        StringBuilder sb = new StringBuilder();
        char c = (char) this.xml[pos++];
        if (c == '0') return "author";
        if (c == '1') return "editor";
        if (c == '2') return "journal";
        if (c == '3') return "booktitle";
        if (c == '4') return "year";
        while (pos < this.xml.length && Character.isLetter(c)) {
            sb.append(c);
            c = (char) this.xml[pos++];
        }
        return sb.toString();
    }

    /**
     * Get the tag name of this record's root element.
     *
     * @return The tag name.
     */
    public String getTag() {
        return getTag(1);
    }

    /**
     * Skips over the tag name at the given position in the XML byte array to find the beginning of
     * the following attributes or the closing <code>'&gt;'</code>.
     *
     * @param pos The starting position.
     * @return The position of the following attributes or the closing <code>'&gt;'</code>.
     */
    int skipTag(int pos) {
        char c = (char) this.xml[pos];
        // case: byte is compressed single digit tag identifier
        if (Character.isDigit(c)) {
            c = (char) this.xml[++pos];
        }
        // case: byte is verbatim tag string
        else {
            while (Character.isLetter(c)) {
                c = (char) this.xml[++pos];
            }
        }
        // finally, skip whitespace characters
        while (Character.isSpaceChar(c)) {
            c = (char) this.xml[++pos];
        }
        return pos;
    }

    /**
     * Retrieve the key of this record.
     *
     * @return The key.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Retrieve the mdate String of this record.
     *
     * @return The mdate.
     */
    public String getMdate() {
        if (this.mdateString == null) {
            this.mdateString = String.format("%04d-%02d-%02d", this.mdate
                    / 10000, (this.mdate % 10000) / 100, this.mdate % 100).intern();
        }
        return this.mdateString;
    }

    /**
     * Get a new (name,value) map for the XML attributes in the byte array starting at a given
     * position.
     * <p>
     * The parsing ends as soon as the first non-attribute character is encountered (e.g., a closing
     * <code>'&gt;'</code>)
     *
     * @param pos The position of the first attribute in the XML byte array.
     * @return The (name,value) map.
     */
    Map<String, String> collectAttributes(int pos) {
        Map<String, String> nameValueMap = new TreeMap<>();
        StringBuilder sb = new StringBuilder();

        char c = (char) this.xml[pos];
        do {
            // collect attribute name
            while (Character.isLetter(c)) {
                sb.append(c);
                c = (char) this.xml[++pos];
            }
            String name = sb.toString();

            // check strict format: name="value"
            if (c != '=') return nameValueMap;
            c = (char) this.xml[++pos];
            if (c != '"') return nameValueMap;
            c = (char) this.xml[++pos];

            // collect attribute value
            sb.setLength(0);
            while (c != '"') {
                sb.append(c);
                c = (char) this.xml[++pos];
            }
            // store (name,value) in map
            nameValueMap.put(name, sb.toString());

            // finally, skip whitespace characters
            c = (char) this.xml[++pos];
            while (Character.isSpaceChar(c)) {
                c = (char) this.xml[++pos];
            }
            sb.setLength(0);
        }
        while (Character.isLetter(c));

        return nameValueMap;
    }

    /**
     * Checks whether this record's root element has any attributes beside the mandatory 'key' and
     * 'mdate' attributes.
     *
     * @return <code>true</code> if the root element has any additional attributes, otherwise
     *         <code>false</code>.
     */
    public boolean hasAdditionalAttribute() {
        int pos = skipTag(1);
        return Character.isLetter((char) this.xml[pos]);
    }

    /**
     * Get a (name,value) map of the additional attributes in this record's root element beside the
     * mandatory 'key' and 'mdate' attributes.
     * <p>
     * The parsing ends as soon as the first non-attribute character is encountered (e.g., a closing
     * <code>'&gt;'</code>)
     *
     * @return The (name,value) map.
     */
    Map<String, String> getAdditionalAttributes() {
        return collectAttributes(skipTag(1));
    }

    /**
     * Returns a sequential Stream of the (name,value) map entries of the additional attributes in
     * this record's root element beside the mandatory 'key' and 'mdate' attributes.
     *
     * @return The stream of map entries.
     */
    Stream<Map.Entry<String, String>> additionalAttributes() {
        return getAdditionalAttributes().entrySet().stream();
    }

    /**
     * Get a (name,value) map of all attributes in this record's root element.
     * <p>
     * This map always includes the mandatory attributes {@code key} and {@code mdate}.
     *
     * @return The (name,value) map.
     */
    public Map<String, String> getAttributes() {
        Map<String, String> attr = getAdditionalAttributes();
        attr.put("key", getKey());
        attr.put("mdate", getMdate());
        return attr;
    }

    /**
     * Returns a sequential Stream of the (name,value) map entries of all attributes in this
     * record's root element.
     *
     * @return The stream of map entries.
     */
    public Stream<Map.Entry<String, String>> attributes() {
        return getAttributes().entrySet().stream();
    }

    /**
     * Escapes only <code>'&amp;','&lt;','&gt;'</code> in the given String. Does not escape
     * <code>'\&apos;','&quot;'</code> or any other characters.
     *
     * @param str The String to escape.
     * @return The escaped String.
     */
    private String escapeXmlEntities(String str) {
        if (str.indexOf('&') < 0 && str.indexOf('<') < 0 && str.indexOf('>') < 0) return str;
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while (pos < str.length()) {
            char c = str.charAt(pos++);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns a standardized XML fragment for the mandatory 'key' and 'mdate' attributes for this
     * record; including the leading space, but without the closing '&gt;'.
     *
     * @return The XML fragment.
     */
    protected String recordAttributes() {
        return String.format(" key=\"%s\" mdate=\"%s\"", this.key, getMdate());
    }

    /**
     * Injects the given <code>tag</code> at the given position in the StringBuilder object,
     * overwriting the single placeholder digit at <code>pos+1</code>. Also injects the closing tag.
     * Between opening and closing tag, <code>value</code> is inserted with {@code '&','<','>'}
     * characters escaped.
     *
     * @param sb The StringBuilder.
     * @param pos The position of the opening {@code <} of the compressed placeholder tag.
     * @param tag The actual tag name to inject.
     * @param value The text content.
     */
    protected void fillPlaceholderTag(StringBuilder sb, int pos, String tag, String value) {
        sb.replace(pos + 1, pos + 2, tag);
        pos = sb.indexOf("/>", pos + 2);
        sb.replace(pos, pos + 2, ">" + escapeXmlEntities(value) + "</" + tag + ">");
    }

    /**
     * Build and return the complete dblp XML element of this record as a String from the compressed
     * byte array stored in this record.
     * <p>
     * Non-ASCII characters in the text content of the returned XML are not escaped (i.e., it's
     * encoded as Java UTF-16).
     *
     * @return The complete dblp record as a String.
     */
    protected StringBuilder xmlBuild() {
        StringBuilder sb = new StringBuilder(new String(this.xml, UTF8));

        // skip the record's root element tag name
        int pos = 1;
        while (Character.isLetter(sb.charAt(pos)))
            pos++;

        // inject root attributes fragment
        sb.insert(pos, recordAttributes());

        // replace compressed field tag names and inject field values
        int index = 0;
        while ((pos = sb.indexOf("<0")) >= 0)
            fillPlaceholderTag(sb, pos, "author", this.names[index++].name());
        while ((pos = sb.indexOf("<1")) >= 0)
            fillPlaceholderTag(sb, pos, "editor", this.names[index++].name());
        return sb;
    }

    /**
     * Return the dblp XML element of this record as a String.
     * <p>
     * Non-ASCII characters in the text content of the returned XML are not escaped (i.e., it's
     * encoded as Java UTF-16).
     *
     * @return The dblp record as a String.
     */
    abstract public String getXml();

    /**
     * Returns a FieldReader for this record.
     *
     * @return The FieldReader.
     */
    public FieldReader getFieldReader() {
        return FieldReader.of(this);
    }

    // /**
    // * Return the first field matching one of the given field tag names, if any.
    // *
    // * @param tags The tag names used to select the field.
    // * @return The matching field, or an empty optional if no such field exists.
    // */
    // public Optional<Field> getField(String... tags) {
    // return getFieldReader().getField(tags);
    // }

    /**
     * Retrieves an unmodifiable collection view of all fields in this record.
     *
     * @return The fields.
     */
    public Collection<Field> getFields() {
        return getFieldReader().getFields();
    }

    /**
     * Retrieves an unmodifiable collection view of all fields matching one of the given field tag
     * names.
     *
     * @param tags The tag names used to select the fields.
     * @return The matching fields.
     */
    public Collection<Field> getFields(String... tags) {
        return getFieldReader().getFields(tags);
    }

    /**
     * Returns a sequential stream of all fields contained in this record.
     *
     * @return The stream of Fields.
     */
    public Stream<Field> fields() {
        return getFieldReader().fields();
    }

    /**
     * Returns a sequential stream of all fields matching one of the given field tag names.
     *
     * @param tags The tag names used to select the fields.
     * @return The stream of matching fields.
     */
    public Stream<Field> fields(String... tags) {
        return getFieldReader().fields(tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() != obj.getClass()) return false;

        if (obj instanceof Record) {
            Record other = (Record) (obj);
            return this.getKey().equals(other.getKey());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.getClass().getName() + " " + this.getKey()).hashCode();
    }

    @Override
    public String toString() {
        return this.key;
    }
}
