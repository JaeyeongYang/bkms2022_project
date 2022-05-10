package org.dblp.mmdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * A class representing a person name in dblp records of any type.
 */
public class PersonName implements Comparable<PersonName> {

    /** The complete dblp name string. */
    private final String name;
    /** The person record associated with this name. */
    protected Person pers;
    /** The parsing of this person name, initialized lazy. */
    private PersonNameParsing parsing;

    /**
     * Creates a new person name.
     *
     * @param name The name.
     * @throws NullPointerException if {@code name == null}.
     * @throws InvalidPersonNameException if an irregular name has been given.
     */
    public PersonName(String name) throws NullPointerException, InvalidPersonNameException {
        if (name == null) throw new NullPointerException();
        this.name = name;
        this.pers = null;
        this.parsing = null; // lazy parsing
    }

    /**
     * Get the complete name string.
     *
     * @param withHomonymId If true, the homonym id number will be given, otherwise it is dropped.
     * @return The name string.
     */
    public String name(boolean withHomonymId) {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.name(withHomonymId);
    }

    /**
     * Get the complete name string, including any possible homonym numbers.
     *
     * @return The name string.
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns a reduced string representation of the name with all abbreviated name parts,
     * suffices, homonym numbers, etc stripped away.
     * <p>
     * E.g.:
     * <ul>
     * <li>{@code "Wei Wang 0017"} will be returned as {@code "Wei Wang"}
     * <li>{@code "Alfred O. Hero III"} will be returned as {@code "Alfred Hero"}
     * </ul>
     *
     * @return A reduced name string.
     */
    public String coreName() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.coreName();
    }

    /**
     * Get the first name(s) of this name. Note that this method does not necessarily classify first
     * names correctly, but according to the conventions in dblp.
     *
     * @return The first name string, or {@code null} if this name does not have a first name.
     */
    public String first() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.first;
    }

    /**
     * Get the last name of this name. Note that this method does not necessarily classify last
     * names, but according to the conventions in dblp.
     *
     * @return The last name string.
     */
    public String last() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.last;
    }

    /**
     * Get the name suffix (i.e., {@code Jr.}, {@code III}, etc.) of this name.
     *
     * @return The name suffix string, or {@code null} if this name does not have a name suffix.
     */
    public String suffix() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.suffix;
    }

    /**
     * Get the 4-digit homonym number of this name.
     *
     * @return The homonym number, or {@code null} if this name does not have such a number.
     */
    public String idnr() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.idnr;
    }

    /**
     * Get the dblp URL part of this name.
     *
     * @return The URL part.
     */
    public String urlpt() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.urlpt;
    }

    /**
     * Returns the CompleteSearch facet identifier string of this name.
     *
     * @return The CS facet ID.
     */
    public String csFacetId() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.csFacetId();
    }

    /**
     * Associates the given person record with this person name.
     *
     * @param pers The person record.
     */
    protected void setPerson(Person pers) {
        this.pers = pers;
    }

    /**
     * Get the complete dblp name string, including any possible homonym numbers.
     *
     * @deprecated Use {@link #name()} instead.
     * @return The name string.
     */
    @Deprecated
    public String getName() {
        return this.name;
    }

    /**
     * Checks whether this is a explicitly disambiguated homonym, that is, a name with a 4-digit
     * homonym number at its end.
     * <p>
     * Note that the base person name of a family of homonymous names (e.g., {@code "Wei Wang"} for
     * {@code "Wei Wang 0001", "Wei Wang 0002",} and so on) will evaluate to {@code false} since it
     * is not an explicitly homonymous name.
     *
     * @return The base name string.
     */
    public boolean isHomonym() {
        if (this.parsing == null) this.parsing = new PersonNameParsing(this.name);
        return this.parsing.idnr != null;
        // return this.name.matches(".* [0-9][0-9][0-9][0-9]");
    }

    /**
     * Checks whether there is a person record associated with this person name.
     *
     * @return {@code true} if a person record is associated, otherwise {@code false}.
     */
    public boolean hasPerson() {
        return this.pers != null;
    }

    /**
     * Get the person record associated with this person name, or {@code null} if no person record
     * has been associated with this name.
     *
     * @return The person record.
     */
    public Person getPerson() {
        return this.pers;
    }

    /**
     * Get the primary name of this person. That is, if this person name has no person record
     * linked, or if this person name is the primary name of the linked person, then this method
     * will return a pointer to this person name. Otherwise &ndash; i.e., f this is an alias name
     * &ndash; the primary person name of the linked person record will be returned.
     *
     * @return The primary person name.
     */
    public PersonName getPrimaryName() {
        try {
            return this.pers.getPrimaryName();
        }
        catch (NullPointerException ex) {
            return this;
        }
    }

    /**
     * Checks whether this is a primary name of a person in dblp, or an alias name.
     * <p>
     * If this person name has not been associated with a person record using
     * {@link #setPerson(Person)} before calling this method, this method will always return
     * <code>false</code>.
     *
     * @return <code>true</code> if this is a primary name, otherwise <code>false</code>.
     */
    public boolean isPrimary() {
        if (this.pers == null) return false;
        PersonName names[] = this.pers.getNamesArray();
        if (names == null || names.length == 0) return false;
        return this == names[0];
    }

    /**
     * Checks whether this person name has alias names in dblp. If no person record is associated
     * with this name this method will always return {@code false}.
     *
     * @return {@code true} iff this person name has alias names in dblp.
     */
    public boolean hasAliases() {
        try {
            return this.pers.hasAliases();
        }
        catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Retrieves all alias names of this person name, minus this name. If no person record is
     * associated with this name this method will always return an empty collection.
     *
     * @return The alias names minus this name.
     */
    public Collection<PersonName> getAliases() {
        try {
            if (!this.pers.hasAliases()) return Collections.emptySet();
            PersonName[] names = this.pers.getNamesArray();
            List<PersonName> aliases = new ArrayList<>(names.length - 1);
            for (PersonName alias : names) {
                if (alias != this) aliases.add(alias);
            }
            return aliases;
        }
        catch (NullPointerException ex) {
            return Collections.emptySet();
        }
    }

    /**
     * Checks whether this name and the other name are aliases of the same person in dblp.
     * <p>
     * If either of the given person names has not been associated with a person record using
     * {@link #setPerson(Person)} prior to calling this method, this method will return {@code true}
     * if and only if both {@link #name} strings are equal.
     *
     * @param other The other person name.
     * @return <code>true</code> if both names are aliases of the same person, otherwise
     *         <code>false</code>.
     */
    public boolean isAliasOf(PersonName other) {
        if (this.equals(other)) return true;

        else if (this.pers == null && other.pers == null) {
            return this.name.equals(other.name);
        }
        else if (this.pers != null && other.pers == null) {
            for (PersonName alias : this.pers.getNamesArray()) {
                if (alias.name.equals(other.name)) return true;
            }
            return false;
        }
        else if (this.pers == null && other.pers != null) {
            for (PersonName alias : other.pers.getNamesArray()) {
                if (alias.name.equals(this.name)) return true;
            }
            return false;
        }
        else return this.pers.equals(other.pers);
    }

    /** The base ASCII character map for all Latin-1 characters from code point 192 to 255. */
    static private final String asciiMap[] =
            { "A", "A", "A", "A", "A", "A", "AE", "C", "E", "E", "E", "E", "I", "I", "I", "I", "D",
                    "N", "O", "O", "O", "O", "O", "", "O", "U", "U", "U", "U", "Y", "Th", "ss", "a",
                    "a", "a", "a", "a", "a", "ae", "c", "e", "e", "e", "e", "i", "i", "i", "i", "d",
                    "n", "o", "o", "o", "o", "o", "/", "o", "u", "u", "u", "u", "y", "th", "y" };

    /**
     * Maps a given string with Latin-1 characters to its ASCII sorting equivalent.
     *
     * @param str The input string.
     * @return The mapped input.
     */
    private static String mapAscii(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < str.length(); pos++) {
            char ch = str.charAt(pos);
            // FIXME: This will ignore unexpected, non-Latin-1 characters
            if (ch >= 'a' && ch <= 'z') sb.append(ch);
            else if (ch >= 'A' && ch <= 'Z') sb.append(ch);
            else if (ch == ' ' || ch == '_') sb.append(' ');
            else if (ch == '-' || ch == '.' || ch == '/' || ch == '@') sb.append(ch);
            else if (ch >= 192 && ch <= 255) sb.append(asciiMap[ch - 192]);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(PersonName other) {
        if (other == null) throw new NullPointerException("cannot compare to null");
        if (this == other) return 0;

        // sort by last name
        int result = mapAscii(this.last()).compareToIgnoreCase(mapAscii(other.last()));
        if (result != 0) return result;

        // sort by first name
        if (this.first() == null && other.first() != null) return -1;
        if (this.first() != null && other.first() == null) return 1;
        if (this.first() != null && other.first() != null) {
            result = mapAscii(this.first()).compareToIgnoreCase(mapAscii(other.first()));
            if (result != 0) return result;
        }
        // sort by suffix
        if (this.suffix() == null && other.suffix() != null) return -1;
        if (this.suffix() != null && other.suffix() == null) return 1;
        if (this.suffix() != null && other.suffix() != null) {
            result = mapAscii(this.suffix()).compareToIgnoreCase(mapAscii(other.suffix()));
            if (result != 0) return result;
        }
        // sort by homonym ID
        if (this.idnr() != null && other.idnr() != null) {
            result = mapAscii(this.idnr()).compareToIgnoreCase(mapAscii(other.idnr()));
            if (result != 0) return result;
        }
        return this.name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return this.name;
    }

}
