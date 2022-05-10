package org.dblp.mmdb.datastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dblp.DblpInterface;
import org.dblp.mmdb.BookTitle;
import org.dblp.mmdb.JournalTitle;
import org.dblp.mmdb.Mmdb;
import org.dblp.mmdb.MmdbInterface;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.Redirect;
import org.dblp.mmdb.TableOfContents;
import org.xml.sax.SAXException;


/**
 * Provides default implementations for an MMDB based coauthor graph implementation of the
 * {@link DblpInterface}.
 *
 * @author mra
 */
@Deprecated
public abstract class AbstractCoauthorGraph implements DblpInterface {

    /** The MMDB. */
    protected final MmdbInterface mmdb;

    /**
     * Creates an MMDB based coauthor graph from an existing MMDB.
     *
     * @param mmdb The MMDB.
     */
    protected AbstractCoauthorGraph(MmdbInterface mmdb) {
        this.mmdb = mmdb;
    }

    /**
     * Creates an MMDB based coauthor graph from the given XML InputStream, assuming the DTD file is
     * present in the class path.
     *
     * @param input The dblp XML input stream.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    protected AbstractCoauthorGraph(InputStream input) throws IOException, SAXException {
        this.mmdb = new Mmdb(input, false);
    }

    /**
     * Creates an MMDB based coauthor graph from the given InputStreams.
     *
     * @param xmlInput The dblp XML input stream.
     * @param dtdInput The dblp DTD input stream.
     * @throws IOException if any IO errors occur while parsing the XML.
     * @throws SAXException if any SAX errors occur while parsing the XML.
     */
    protected AbstractCoauthorGraph(InputStream xmlInput, InputStream dtdInput)
            throws IOException, SAXException {
        this.mmdb = new Mmdb(xmlInput, dtdInput, false);
    }

    /**
     * Builds a new collection of all direct coauthors of the given person.
     *
     * @param person The center person.
     * @return A collection of the person's coauthors.
     */
    protected static Collection<Person> collectCoauthors(Person person) {
        Set<Person> coauths =
                person.publications().flatMap(Publication::names).map(PersonName::getPerson).filter(Objects::nonNull).collect(Collectors.toSet());
        coauths.remove(person);
        return coauths;
    }

    @Override
    public boolean hasPublication(String key) {
        return this.getPublication(key) != null;
    }

    @Override
    public Publication getPublication(String key) {
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
    public boolean hasPerson(String key) {
        return this.getPerson(key) != null;
    }

    @Override
    public Person getPerson(String key) {
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
    public boolean hasPersonByName(String name) {
        return this.getPersonByName(name) != null;
    }

    @Override
    public Person getPersonByName(String name) {
        try {
            return this.mmdb.getPersonName(name).getPerson();
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

    @Override
    public boolean hasToc(String key) throws NullPointerException {
        return this.getToc(key) != null;
    }

    @Override
    public TableOfContents getToc(String key) throws NullPointerException {
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

    @Override
    public BookTitle getBookTitle(String title) throws NullPointerException {
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

    @Override
    public JournalTitle getJournal(String title) throws NullPointerException {
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

}
