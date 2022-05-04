package org.dblp.mmdb;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;


/**
 * A class representing a field inside a dblp {@link Record}.
 *
 * @author Michael Ley
 * @see FieldReader
 */
public class Field {

    /** The tag of this field. */
    private final String tag;
    /** The attributes of this field as key-value map. */
    private final Map<String, String> attributes;
    /**
     * The value of this field. That is, the entity-encoded text content plus any (possibly nested)
     * formatting elements like {@code <i>}, {@code <tt>}, {@code <sub>}, or {@code <sup>}.
     */
    private final String value;

    /**
     * Creates a new field.
     *
     * @param tag The tag.
     * @param attributes The attributes as key-value map.
     * @param value The field value as string.
     */
    protected Field(String tag, Map<String, String> attributes, String value) {
        this.tag = tag;
        this.attributes = attributes;
        this.value = value;
    }

    /**
     * Retrieves the tag of this field.
     *
     * @return The tag.
     */
    public String tag() {
        return this.tag;
    }

    /**
     * Retrieves the value of this field. That is, the entity-encoded text content plus any
     * (possibly nested) formatting elements like {@code <i>}, {@code <tt>}, {@code <sub>}, or
     * {@code <sup>}.
     *
     * @return The value.
     */
    public String value() {
        return this.value;
    }

    /**
     * Checks whether this field has any attributes.
     *
     * @return {@code true} if this field has any attributes, otherwise {@code false}.
     */
    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    /**
     * Checks whether this field has the given attribute.
     *
     * @param attr The attribute name.
     * @return {@code true} if this field has the given attribute, otherwise {@code false}.
     */
    public boolean hasAttribute(String attr) {
        return this.attributes.containsKey(attr);
    }

    /**
     * Retrieves the requested attribute from this field.
     *
     * @param attr The attribute name.
     * @return The attribute value, or {@code null} if there is no such attribute.
     */
    public String attribute(String attr) {
        return this.attributes.get(attr);
    }

    /**
     * Retrieves an unmodifiable map view the attribute map of this field.
     *
     * @return The attributes as key-value map.
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(this.attributes);
    }

    /**
     * Retrieves a sequential stream of attributes of this field. Elements of this stream are the
     * key-value map entries.
     *
     * @return The stream of attribute key-value map entries.
     */
    public Stream<Map.Entry<String, String>> attributes() {
        return this.attributes.entrySet().stream();
    }

}
