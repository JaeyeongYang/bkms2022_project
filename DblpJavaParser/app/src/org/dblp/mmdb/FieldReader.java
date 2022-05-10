package org.dblp.mmdb;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * A convenience class for accessing the field values and attributes of a dblp {@link Record}. This
 * class automatically manages decoding the internal space-efficient record representation into
 * regular {@link String}s.
 * <p>
 * Example usage for unique fields: <pre>
 * FieldReader reader = record.getFieldReader();
 * String textContent = reader.valueOf(tagName);
 * String note = reader.attributesOf(tagName).get("note");
 * // do stuff</pre>
 * <p>
 * Example usage for repeating fields: <pre>
 * FieldReader reader = record.getFieldReader();
 * for( Field field : reader.getFields(tagName) ) {
 *     String textContent = field.value();
 *     String note = field.attribute("note");
 *     // do stuff
 * }</pre>
 *
 * @author Michael Ley
 * @author Marcel R. Ackermann
 * @see Field
 */
public class FieldReader {

    /** The charset to use. */
    static final private Charset UTF8 = Charset.forName("UTF-8");
    /** The global field reader cache. */
    static final private Map<Record, FieldReader> cache = new HashMap<>();

    /** The record object. */
    private Record rec;
    /**
     * The array of start positions (inclusive) of the field tag characters within XML byte stream
     * representation of the record, as build by to {@link #buildTagindex()}.
     */
    private int[] tagIndex;
    /**
     * The array of start (inclusive) and end positions (exclusive) of the field value characters
     * within XML byte stream representation of the record, as build by to {@link #buildTagindex()}.
     */
    private int[] valueIndex;

    /**
     * Create a new, empty FieldReader. This constructor is intended only for extensions, and the
     * implementing class has to take care of initializing all member fields properly.
     */
    protected FieldReader() {}

    /**
     * Creates a new field reader for the given dblp record.
     *
     * @param rec The dblp record.
     */
    private FieldReader(Record rec) {
        this.rec = rec;
    }

    /**
     * Retrieve the field reader from cache or create and store a new one for the given dblp record.
     *
     * @param rec The dblp record.
     * @return The field reader.
     */
    static FieldReader of(Record rec) {
        FieldReader reader = cache.get(rec);
        if (reader == null) {
            reader = new FieldReader(rec);
            cache.put(rec, reader);
        }
        return reader;
    }

    /**
     * Build indices for the starting and ending positions of tag names and field values in the XML
     * byte stream representation of the record.
     * <p>
     * As {@link #tagIndex}, the array of start positions (inclusive) within {@link Record#xml} are
     * stored. The start position of the {@code i}-th field tag name is stored as
     * {@code tagIndex[i]}.
     * <p>
     * AS {@link #valueIndex}, the array of start (inclusive) and end positions (exclusive) within
     * The start position of the {@code i}-th field value is stored as {@code valueIndex[2*i]},
     * while the end position is stored as {@code valueIndex[2*i+1]}. If the start position is
     * negative, then the value is not retrieved from the XML byte stream representation, but from
     * the record object {@code rec}, depending on context.
     * <table>
     * <tr>
     * <th>startPos</th>
     * <th>value</th>
     * </tr>
     * <tr>
     * <td>-1</td>
     * <td>{@code rec.nameAt(endPos).getName()}</td>
     * </tr>
     * <tr>
     * <td>-2</td>
     * <td>{@code Integer.toString(((Publication)rec).getYear())}</td>
     * </tr>
     * <tr>
     * <td>-3</td>
     * <td>{@code ((Publication)rec).getBooktitle().getTitle()}</td>
     * </tr>
     * <tr>
     * <td>-4</td>
     * <td>{@code ((Publication)rec).getJournal().getTitle()}</td>
     * </tr>
     * </table>
     */
    private void buildTagindex() {
        ArrayList<Integer> tagList = new ArrayList<>();
        ArrayList<Integer> valueList = new ArrayList<>();

        byte[] xml = this.rec.getXmlBytes();
        int len = xml.length;
        int personNameCount = 0;
        int pos = 0, level = 0, state = 0;
        while (pos < len) {
            byte b = xml[pos++];
            if (state == 0) {
                if (b == '<') {
                    state = 1;
                }
                continue;
            }
            if (state == 1) {
                if (b == '/') {
                    level--;
                    if (level == 1) valueList.add(pos - 2);
                    state = 0;
                }
                else {
                    level++;
                    if (level == 2) {
                        tagList.add(pos - 1);
                        switch (b) {
                            case '0':
                            case '1':
                                valueList.add(-1);
                                valueList.add(personNameCount++);
                                break;
                            case '2':
                                valueList.add(-2);
                                valueList.add(0);
                                break;
                            case '3':
                                valueList.add(-3);
                                valueList.add(0);
                                break;
                            case '4':
                                valueList.add(-4);
                                valueList.add(0);
                                break;
                        }
                    }
                    state = 2;
                }
                continue;
            }
            if (state == 2) {
                if (b == '/') {
                    level--;
                    state = 0;
                }
                else if (b == '>') {
                    state = 0;
                    if (level == 2) valueList.add(pos);
                }
                else if (b == '"') {
                    state = 3;
                }
                continue;
            }
            if (state == 3) {
                if (b == '"') state = 2;
                continue;
            }
        }
        this.tagIndex = new int[tagList.size()];
        for (int i = 0; i < this.tagIndex.length; i++) {
            this.tagIndex[i] = tagList.get(i).intValue();
        }
        this.valueIndex = new int[valueList.size()];
        for (int i = 0; i < this.valueIndex.length; i++) {
            this.valueIndex[i] = valueList.get(i).intValue();
        }
    }

    // FIXME: change from deprecation to package or private visibility

    /**
     * Retrieves the number of fields of the given record.
     *
     * @return The number of fields.
     * @deprecated Use {@link #numberOfFields()} for same result.
     */
    @Deprecated
    public int getNumberOfFields() {
        if (this.tagIndex == null) buildTagindex();
        return this.tagIndex.length;
    }

    /**
     * Retrieves the tag name of the {@code i}-th field of the given record.
     *
     * @param i The field index.
     * @return The tag name string
     * @throws IndexOutOfBoundsException if {@code i < 0 || i >= getNumberOfFields()}.
     * @deprecated Use {@link Field#tag()} for same result. Instead of <pre>{@literal
     * for( int i=0; i < getNumberOfFields(); i++ ) {
     *     if getTag(i).equals(tag) {
     *        ...
     *     }
     * }}</pre> idiom, use {@link #getFields(String[])} or {@link #fields(String[])}.
     */
    @Deprecated
    public String getTag(int i) throws IndexOutOfBoundsException {
        if (this.tagIndex == null) buildTagindex();
        return this.rec.getTag(this.tagIndex[i]);
    }

    /**
     * Checks whether the {@code i}-th field of the given record has any attributes.
     *
     * @param i The field index.
     * @return {@code true} if the {@code i}-th field has any attribute, otherwise {@code false} .
     *         If {@code i < 0 || i >= getNumberOfFields()} then this method will always return
     *         {@code false}.
     * @deprecated Use {@link Field#attribute(String)} for same result.
     */
    @Deprecated
    public boolean hasAttribute(int i) {
        if (this.tagIndex == null) buildTagindex();
        if (i < 0 || i >= this.tagIndex.length) return false;
        int pos = this.rec.skipTag(this.tagIndex[i]);
        return Character.isLetter((char) this.rec.getXmlBytes()[pos]);
    }

    /**
     * Retrieves the attributes of the {@code i}-th field of the given record as key-value map.
     *
     * @param i The field index.
     * @return The attributes as key-value map entry.
     * @throws IndexOutOfBoundsException if {@code i < 0 || i >= getNumberOfFields()}.
     * @deprecated Use {@link Field#getAttributes()} for same result.
     */
    @Deprecated
    public Map<String, String> getAttributes(int i) throws IndexOutOfBoundsException {
        if (this.tagIndex == null) buildTagindex();
        return this.rec.collectAttributes(this.rec.skipTag(this.tagIndex[i]));
    }

    // /**
    // * Retrieves a sequential stream of attributes of the {@code i}-th field of the given record.
    // * Elements of this stream are the key-value map entries.
    // *
    // * @param i The field index.
    // * @return The stream of attribute key-value map entries.
    // * @throws IndexOutOfBoundsException if {@code i < 0 || i >= getNumberOfFields()}.
    // * @deprecated Use {@link Field#attributes()} for same result.
    // */
    // @Deprecated
    // public Stream<Map.Entry<String, String>> attributes(int i) throws IndexOutOfBoundsException {
    // return getAttributes(i).entrySet().stream();
    // }

    /**
     * Retrieves the field value of the {@code i}-th field of the given record. That is, the
     * entity-encoded text content plus any (possibly nested) formatting elements like {@code <i>},
     * {@code <tt>}, {@code <sub>}, or {@code <sup>}.
     *
     * @param i The field index.
     * @return The field value string.
     * @throws IndexOutOfBoundsException if {@code i < 0 || i >= getNumberOfFields()}.
     * @deprecated Use {@link Field#value()} for the same result.
     *             <p>
     *             Instead of the <pre>{@literal
     * for( int i=0; i < getNumberOfFields(); i++ ) {
     *     if getTag(i).equals(tag) {
     *        ...
     *     }
     * }}</pre> idiom, use {@link #getFields(String[])} or {@link #fields(String[])}.
     */
    @Deprecated
    public String getValue(int i) throws IndexOutOfBoundsException {
        if (this.tagIndex == null) buildTagindex();
        if (i < 0 || i >= this.tagIndex.length) throw new IndexOutOfBoundsException();
        int startPos = this.valueIndex[i * 2];
        int endPos = this.valueIndex[i * 2 + 1];
        if (startPos >= 0)
            return new String(this.rec.getXmlBytes(), startPos, endPos - startPos, UTF8);
        if (startPos == -1) return this.rec.nameAt(endPos).name();
        if (this.rec instanceof Publication) {
            Publication publ = (Publication) this.rec;
            if (startPos == -4) return Integer.toString(publ.getYear());
            if (startPos == -3) return publ.getBooktitle().getTitle();
            if (startPos == -2) return publ.getJournal().getTitle();
        }
        return null;
    }

    /**
     * Retrieves the index of the first occurrence of the field tag in the given record.
     *
     * @param tag The tag.
     * @return The index of the first occurrence, or {@code -1} if there is no such field.
     * @deprecated Use of tag numbers is discontinued.
     *             <p>
     *             Instead of the <pre>{@literal
     * for( int i=0; i < getNumberOfFields(); i++ ) {
     *     if getTag(i).equals(tag) {
     *        ...
     *     }
     * }</pre> idiom, use {@link #getFields(String[])} or {@link #fields(String[])}.
     *             <p>
     *             Instead of the <pre>
     * if( indexOf(tag) >= 0) {
     *    ...
     * }}</pre> idiom, use {@link #contains(String)}.
     */
    @Deprecated
    public int indexOf(String tag) {
        return indexOf(tag, 0);
    }

    /**
     * Retrieves the index of the first occurrence of the field tag in the given record, starting at
     * the specified index.
     *
     * @param tag The tag.
     * @param fromIndex The index from which to start the search.
     * @return The index of the first occurrence, starting at the specified index, or {@code -1} if
     *         there is no such field. If {@code fromIndex < 0 || fromIndex >= getNumberOfFields()}
     *         then this method always returns {@code -1}.
     * @deprecated Use of tag numbers is discontinued.
     *             <p>
     *             Instead of the <pre>{@literal
     * int index = fields.indexOf(tag);
     * while (index > -1) {
     *     ...
     *     index = fields.indexOf(tag, index + 1);
     * }
     * }</pre> idiom, use {@link #getFields(String[])} or {@link #fields(String[])}.
     *             <p>
     *             Instead of the <pre>{@literal
     * if( indexOf(tag) >= 0) {
     *    ...
     * }}</pre> idiom, use {@link #contains(String)}.
     */
    @Deprecated
    public int indexOf(String tag, int fromIndex) {
        if (this.tagIndex == null) buildTagindex();
        if (fromIndex < 0 || fromIndex >= this.tagIndex.length) return -1;
        for (int i = fromIndex; i < this.tagIndex.length; i++) {
            if (this.rec.getTag(this.tagIndex[i]).equals(tag)) return i;
        }
        return -1;
    }

    // new public interface follows below

    /**
     * Checks whether the given record contains a field of the given tag name.
     *
     * @param tag The field tag.
     * @return {@code true} if such a field exists in the record, otherwise {@code false}.
     */
    public boolean contains(String tag) {
        return indexOf(tag) >= 0;
    }

    /**
     * Checks whether the given record contains the queried field tag and value pair.
     *
     * @param tag The field tag.
     * @param value The field value. That is, the entity-encoded text content plus any (possibly
     *            nested) formatting elements like {@code <i>}, {@code <tt>}, {@code <sub>}, or
     *            {@code <sup>}.
     * @return {@code true} if such a pair exists in the record, otherwise {@code false}.
     */
    public boolean contains(String tag, String value) {
        for (int i = indexOf(tag); i > -1; i = indexOf(tag, ++i)) {
            if (getValue(i).equals(value)) return true;
        }
        return false;
    }

    /**
     * Retrieves the field value of the first occurrence of the field tag in the given record. That
     * is, the entity-encoded text content plus any (possibly nested) formatting elements like
     * {@code <i>}, {@code <tt>}, {@code <sub>}, or {@code <sup>}.
     *
     * @param tag The tag.
     * @return The field value string, or {@code null} if no such field exists.
     */
    public String valueOf(String tag) {
        int i = indexOf(tag);
        if (i < 0) return null;
        return getValue(i);
    }

    /**
     * Retrieves the attributes of the first occurrence of the field tag in the given record as
     * key-value map.
     *
     * @param tag The tag.
     * @return The attributes as key-value map entry, or {@code null} if no such field exists.
     */
    public Map<String, String> getAttributesOf(String tag) {
        int i = indexOf(tag);
        if (i < 0) return null;
        return getAttributes(i);
    }

    /**
     * Retrieves a sequential stream of the attributes of the first occurrence of the field tag in
     * the given record. Elements of this stream are the key-value map entries.
     *
     * @param tag The tag.
     * @return The attributes as key-value map entry, or {@code null} if no such field exists.
     */
    public Stream<Map.Entry<String, String>> attributesOf(String tag) {
        try {
            return getAttributesOf(tag).entrySet().stream();
        }
        catch (NullPointerException ex) {
            return null;
        }
    }

    /**
     * Returns a new field object of the {@code i}-th field of the given record.
     *
     * @param i The field index.
     * @return The field object, or {@code null} if {@code i < 0 || i >= getNumberOfFields()}.
     * @throws IndexOutOfBoundsException if {@code i < 0 || i >= getNumberOfFields()}.
     */
    private Field getField(int i) throws IndexOutOfBoundsException {
        if (this.tagIndex == null) buildTagindex();
        return new Field(this.rec.getTag(this.tagIndex[i]), this.rec.collectAttributes(this.rec.skipTag(this.tagIndex[i])), getValue(i));
    }

    // /**
    // * Return the first field matching one of the given field tag names, if any.
    // *
    // * @param tags The tag names.
    // * @return The matching field, or an empty optional if no such field exists.
    // */
    // public Optional<Field> getField(String... tags) {
    // if (this.tagIndex == null) buildTagindex();
    // for (int i = 0; i < this.tagIndex.length; i++) {
    // for (String tag : tags) {
    // if (getTag(i).equals(tag)) return Optional.of(getField(i));
    // }
    // }
    // return Optional.empty();
    // }

    /**
     * Retrieves an unmodifiable Collection view of all fields in the given record.
     *
     * @return The fields.
     */

    public Collection<Field> getFields() {
        if (this.tagIndex == null) buildTagindex();
        List<Field> result = new ArrayList<>(this.tagIndex.length);
        for (int i = 0; i < this.tagIndex.length; i++) {
            result.add(getField(i));
        }
        return Collections.unmodifiableCollection(result);
    }

    /**
     * Retrieves an unmodifiable Collection view of all fields in the given record matching one of
     * the given field tag names.
     *
     * @param tags The tag names.
     * @return The matching fields.
     */
    public Collection<Field> getFields(String... tags) {
        if (this.tagIndex == null) buildTagindex();
        List<Field> result = new ArrayList<>(2);
        for (int i = 0; i < this.tagIndex.length; i++) {
            for (String tag : tags) {
                if (getTag(i).equals(tag)) {
                    result.add(getField(i));
                    break;
                }
            }
        }
        return Collections.unmodifiableCollection(result);
    }

    /**
     * Retrieves a sequential stream of all fields of the given record.
     *
     * @return The stream of fields.
     */
    public Stream<Field> fields() {
        if (this.tagIndex == null) buildTagindex();
        return IntStream.range(0, this.tagIndex.length).mapToObj(i -> getField(i));
    }

    /**
     * Retrieves a sequential stream of all fields of the given record matching one of the given
     * field tag names.
     *
     * @param tags The tag names.
     * @return The stream of matching fields.
     */
    public Stream<Field> fields(String... tags) {
        if (this.tagIndex == null) buildTagindex();
        Stream<Field> result = Stream.empty();
        for (String tag : tags) {
            result = Stream.concat(result, IntStream.range(0, this.tagIndex.length).filter(i -> this.rec.getTag(this.tagIndex[i]).equals(tag)).mapToObj(i -> getField(i)));
        }
        return result;
    }

    /**
     * Retrieves the number of fields of the given record.
     *
     * @return The number of fields.
     */
    public int numberOfFields() {
        if (this.tagIndex == null) buildTagindex();
        return this.tagIndex.length;
    }

    /**
     * Retrieves the number of fields of the given record matching one of the given field tag names.
     *
     * @param tags The tags.
     * @return The number of matching fields.
     */
    public int numberOfFields(String... tags) {
        if (this.tagIndex == null) buildTagindex();
        int result = 0;
        for (int i = 0; i < this.tagIndex.length; i++) {
            for (String tag : tags) {
                if (getTag(i).equals(tag)) result++;
                break;
            }
        }
        return result;
    }

}
