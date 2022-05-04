/*
 * Copyright (c)2015, dblp Team (University of Trier / Schloss Dagstuhl - Leibniz-Zentrum fuer
 * Informatik GmbH) All rights reserved. Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. * Neither the name of the dblp team nor the names of
 * its contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL DBLP TEAM BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package org.dblp.mmdb.datastructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dblp.DblpInterface;
import org.dblp.mmdb.Mmdb;
import org.dblp.mmdb.MmdbInterface;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.xml.sax.SAXException;


/**
 * A simple coauthor graph implementation. This graph is loaded lazy into main memory whenever any
 * local information is queried.
 */
@Deprecated
public class SimpleLazyCoauthorGraph extends AbstractCoauthorGraph {

    /** The adjacency list of this graph as a map. */
    private final Map<Person, Person[]> graph;
    /** The map of all local coauthor graphs. */
    private final Map<Person, LocalCoauthorsGraph> localMap;
    /** An empty Person array, used as default return value. < */
    private static final Person[] emptyPersArray = new Person[0];
    /** An empty DBLP. Used mainly for testing code without building an actual graph object. */
    public static final DblpInterface EMTPY_DBLP = new SimpleLazyCoauthorGraph(Mmdb.EMTPY_MMDB);

    /**
     * Create a new, empty SimpleLazyCoauthorGraph. The local parts of the actual graph are not
     * materialized before any required local information is queried.
     *
     * @param mmdb The MMDB.
     */
    public SimpleLazyCoauthorGraph(MmdbInterface mmdb) {
        super(mmdb);
        this.graph = new ConcurrentHashMap<>();
        this.localMap = new ConcurrentHashMap<>();
    }

    /**
     * Create a new, empty SimpleLazyCoauthorGraph, assuming the DTD file is present in the class
     * path. The local parts of the actual graph are not materialized before any required local
     * information is queried.
     *
     * @param input The dblp XML input stream.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleLazyCoauthorGraph(InputStream input) throws IOException, SAXException {
        super(input);
        this.graph = new ConcurrentHashMap<>();
        this.localMap = new ConcurrentHashMap<>();
    }

    /**
     * Create a new, empty SimpleLazyCoauthorGraph. The local parts of the actual graph are not
     * materialized before any required local information is queried.
     *
     * @param xmlInput The dblp XML input stream.
     * @param dtdInput The dblp DTD input stream.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleLazyCoauthorGraph(InputStream xmlInput, InputStream dtdInput)
            throws IOException, SAXException {
        super(xmlInput, dtdInput);
        this.graph = new ConcurrentHashMap<>();
        this.localMap = new ConcurrentHashMap<>();
    }

    /**
     * Create a new, empty SimpleLazyCoauthorGraph, assuming the DTD file is present in the class
     * path. The local parts of the actual graph are not materialized before any required local
     * information is queried.
     *
     * @param file The dblp XML input file.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleLazyCoauthorGraph(File file) throws IOException, SAXException {
        this(new FileInputStream(file));
    }

    /**
     * Create a new, empty SimpleLazyCoauthorGraph. The local parts of the actual graph are not
     * materialized before any required local information is queried.
     *
     * @param xmlFile The dblp XML file.
     * @param dtdFile The dblp DTD file.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleLazyCoauthorGraph(File xmlFile, File dtdFile) throws IOException, SAXException {
        this(new FileInputStream(xmlFile), new FileInputStream(dtdFile));
    }

    /**
     * Create a new, empty SimpleLazyCoauthorGraph, assuming the DTD file is present in the class
     * path. The local parts of the actual graph are not materialized before any required local
     * information is queried.
     *
     * @param filename The dblp XML input file name.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleLazyCoauthorGraph(String filename) throws IOException, SAXException {
        this(new FileInputStream(filename));
    }

    /**
     * Create a new, empty SimpleLazyCoauthorGraph. The local parts of the actual graph are not
     * materialized before any required local information is queried.
     *
     * @param xmlFile The dblp XML file name.
     * @param dtdFile The dblp DTD file name.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleLazyCoauthorGraph(String xmlFile, String dtdFile)
            throws IOException, SAXException {
        this(new FileInputStream(xmlFile), new FileInputStream(dtdFile));
    }

    /**
     * Lazy create the given persons as nodes in this coauthor graph.
     *
     * @param persons The persons.
     */
    private void ensurePersons(Person... persons) {
        for (Person person : persons) {
            if (!graph.containsKey(person)) {
                Collection<Person> coauths = collectCoauthors(person);
                graph.put(person, coauths.toArray(new Person[coauths.size()]));
            }
        }
    }

    @Override
    public Collection<Person> getCoauthors(Person person) {
        if (person == null) return Arrays.asList(emptyPersArray);
        this.ensurePersons(person);
        Person[] coauthors = graph.get(person);
        return Collections.unmodifiableList(Arrays.asList(coauthors));
    }

    @Override
    public Stream<Person> coauthors(Person person) {
        if (person == null) return Arrays.stream(emptyPersArray);
        this.ensurePersons(person);
        Person[] coauthors = graph.get(person);
        return Arrays.stream(coauthors);
    }

    @Override
    public int numberOfCoauthors(Person person) {
        if (person == null) return 0;
        this.ensurePersons(person);
        return graph.get(person).length;
    }

    @Override
    public boolean hasCoauthors(Person first, Person second) {
        if (first == null || second == null) return false;
        this.ensurePersons(first, second);
        Person[] coauths1 = graph.get(first);
        Person[] coauths2 = graph.get(second);
        return Arrays.asList(coauths1).contains(second) || Arrays.asList(coauths2).contains(first);
    }

    /**
     * Lazy create the given persons as nodes in this coauthor graph.
     *
     * @param persons The persons.
     */
    private void ensureCoauthorGraph(Person... persons) {
        ensurePersons(persons);
        for (Person person : persons) {
            if (!localMap.containsKey(person)) {
                LocalCoauthorsGraph local = new LocalCoauthorsGraph(this, person);
                localMap.put(person, local);
            }
        }
    }

    @Override
    public int numberOfCoauthorCommunities(Person person) {
        if (person == null) return 0;
        ensureCoauthorGraph(person);
        LocalCoauthorsGraph local = this.localMap.get(person);
        return local.numberOfCommunities();
    }

    @Override
    public Collection<Person> getCoauthorCommunity(Person person, int index)
            throws IndexOutOfBoundsException {
        if (person == null) return Arrays.asList(emptyPersArray);
        ensureCoauthorGraph(person);
        LocalCoauthorsGraph local = this.localMap.get(person);
        return local.getCommunity(index);
    }

    @Override
    public LocalCoauthorsGraph getCoauthorGraph(Person person) throws NullPointerException {
        if (person == null) throw new NullPointerException();
        ensureCoauthorGraph(person);
        LocalCoauthorsGraph local = this.localMap.get(person);
        return local;
    }

    // TODO: testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws IOException, SAXException, InterruptedException {

        if (args.length < 2) {
            System.out.format("Usage: %s <dblp.xml> <dblp.dtd>\n", SimpleLazyCoauthorGraph.class.getSimpleName());
            System.exit(0);
        }

        System.err.print("starting in:");
        for (int i = 10; i > 0; i--) {
            if (i < 10) System.err.print(" -");
            System.err.print(" " + i);
            Thread.sleep(1000);
        }
        System.err.println(" - go!");

        SimpleLazyCoauthorGraph dblp = new SimpleLazyCoauthorGraph(args[0], args[1]);

        System.out.println("dblp.numberOfPublications() = " + dblp.numberOfPublications());
        System.out.println("dblp.numberOfPersons() = " + dblp.numberOfPersons());
        System.out.println();

        String[] keys =
                { "conf/focs/GuruswamiX13", "conf/focs/GopalanMRTV12", "conf/focs/HaramatySS11" };
        for (String key : keys) {
            System.out.println(key);
            System.out.println("dblp.hasPublication(" + key + ") = " + dblp.hasPublication(key));
            Publication publ = dblp.getPublication(key);
            System.out.println("dblp.getPublication(" + key + ") = " + publ);
            System.out.println();
        }

        String[] persKeys = { "homepages/g/VenkatesanGuruswami", "homepages/r/OmerReingold",
                "homepages/w/AviWigderson" };
        for (String key : persKeys) {
            System.out.println(key);
            System.out.println("dblp.hasPerson(" + key + ") = " + dblp.hasPerson(key));
            Person pers = dblp.getPerson(key);
            System.out.println("dblp.getPerson(" + key + ") = " + pers);
            System.out.print("dblp.getCoauthors(" + key + ") = ");
            System.out.println(dblp.coauthors(pers).map(Person::getPrimaryName).map(PersonName::name).collect(Collectors.joining(", ", "[", "]")));
            System.out.println("dblp.numberOfCoauthors(" + key + ") = "
                    + dblp.numberOfCoauthors(pers));
            System.out.println("dblp.numberOfCoauthorCommunities(" + key + ") = "
                    + dblp.numberOfCoauthorCommunities(pers));
            System.out.println();
        }

        String[] people =
                { "Shafi Goldwasser", "Oded Goldreich", "Madhu Sudan", "Russell Impagliazzo" };
        for (String peep : people) {
            System.out.println(peep);
            System.out.println("dblp.hasPersonByName(" + peep + ") = "
                    + dblp.hasPersonByName(peep));
            Person pers = dblp.getPersonByName(peep);
            System.out.println("dblp.getPersonByName(" + peep + ") = " + pers);
            System.out.print("dblp.getCoauthors(" + peep + ") = ");
            System.out.println(dblp.coauthors(pers).map(Person::getPrimaryName).map(PersonName::name).collect(Collectors.joining(", ", "[", "]")));
            System.out.println("dblp.numberOfCoauthors(" + peep + ") = "
                    + dblp.numberOfCoauthors(pers));
            System.out.println("dblp.numberOfCoauthorCommunities(" + peep + ") = "
                    + dblp.numberOfCoauthorCommunities(pers));
            System.out.println();
        }

        System.out.println("max number of coauthors = "
                + dblp.persons().mapToInt(p -> dblp.numberOfCoauthors(p)).max().getAsInt());
        System.out.println("#coauthors: #persons");
        Map<Integer, Long> coauthStats =
                dblp.persons().collect(Collectors.groupingBy(pers -> dblp.numberOfCoauthors(pers), Collectors.counting()));
        coauthStats.entrySet().stream().sorted((Map.Entry<Integer, Long> entry1,
                Map.Entry<Integer, Long> entry2) -> entry1.getKey().compareTo(entry2.getKey())).forEachOrdered(entry -> System.out.println(entry.getKey().toString()
                        + ": " + entry.getValue().toString() + "x"));
        System.out.println();

        System.out.println("max number of communities = "
                + dblp.persons().mapToInt(p -> dblp.numberOfCoauthorCommunities(p)).max().getAsInt());
        System.out.println("#communities: #persons");
        Map<Integer, Long> communityStats =
                dblp.persons().collect(Collectors.groupingBy(pers -> dblp.numberOfCoauthorCommunities(pers), Collectors.counting()));
        communityStats.entrySet().stream().sorted((Map.Entry<Integer, Long> entry1,
                Map.Entry<Integer, Long> entry2) -> entry1.getKey().compareTo(entry2.getKey())).forEachOrdered(entry -> System.out.println(entry.getKey().toString()
                        + ": " + entry.getValue().toString() + "x"));

    }

}
