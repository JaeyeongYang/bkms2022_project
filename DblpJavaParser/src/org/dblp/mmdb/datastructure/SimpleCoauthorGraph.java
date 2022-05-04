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
import org.dblp.mmdb.Publication;
import org.xml.sax.SAXException;


/**
 * A simple coauthor graph implementation. This implementation constructs the whole graph in main
 * memory at time of construction.
 */
@Deprecated
public class SimpleCoauthorGraph extends AbstractCoauthorGraph {

    /** The adjacency list of this graph as a map. */
    private final Map<Person, Person[]> graph;
    /** The map of all local coauthor graphs. */
    private final Map<Person, LocalCoauthorsGraph> localMap;
    /** The number of edges in this graph. */
    private final int edges;
    /** An empty Person array, used as default return value. < */
    private static final Person[] emptyPersArray = new Person[0];
    /** An empty DBLP. Used mainly for testing code without building an actual graph object. */
    public static final DblpInterface EMTPY_DBLP = new SimpleLazyCoauthorGraph(Mmdb.EMTPY_MMDB);

    /**
     * Create a new SimpleCoauthorGraph. The whole graph is materialized in main memory.
     *
     * @param mmdb The MMDB.
     */
    public SimpleCoauthorGraph(MmdbInterface mmdb) {
        super(mmdb);
        this.graph = new ConcurrentHashMap<>();
        this.localMap = new ConcurrentHashMap<>();
        // FIXME: force the construction of the complete graph more explicitly
        this.edges = this.mmdb.persons().parallel().mapToInt(p -> numberOfCoauthors(p)).sum() / 2;
    }

    /**
     * Create a new SimpleCoauthorGraph. The whole graph is materialized in main memory.
     *
     * @param input The dblp XML input stream .
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleCoauthorGraph(InputStream input) throws IOException, SAXException {
        super(input);
        this.graph = new ConcurrentHashMap<Person, Person[]>();
        this.localMap = new ConcurrentHashMap<>();
        // FIXME: force the construction of the complete graph more explicitly
        this.edges = this.mmdb.persons().parallel().mapToInt(p -> numberOfCoauthors(p)).sum() / 2;
    }

    /**
     * Create a new SimpleCoauthorGraph. The whole graph is materialized in main memory.
     *
     * @param file The dblp XML input file.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleCoauthorGraph(File file) throws IOException, SAXException {
        this(new FileInputStream(file));
    }

    /**
     * Create a new SimpleCoauthorGraph. The whole graph is materialized in main memory.
     *
     * @param filename The dblp XML input file name.
     * @throws IOException if any IO errors occur.
     * @throws SAXException if any SAX errors occur during processing
     */
    public SimpleCoauthorGraph(String filename) throws IOException, SAXException {
        this(new File(filename));
    }

    /**
     * Creates a new coauthors array (i.e., adjacency list item) for a not yet seen person and adds
     * it in this graph.
     *
     * @param person A new person, not yet contained in this graph.
     * @return The new coauthors array.
     */
    private Person[] createCouthorsArray(Person person) {
        Collection<Person> c = collectCoauthors(person);
        Person[] coauthors = c.toArray(new Person[c.size()]);
        graph.put(person, coauthors);
        return coauthors;
    }

    @Override
    public Collection<Person> getCoauthors(Person person) {
        if (person == null) return null;
        Person[] coauthors = graph.get(person);
        if (coauthors == null) coauthors = createCouthorsArray(person);
        return Collections.unmodifiableList(Arrays.asList(coauthors));
    }

    @Override
    public Stream<Person> coauthors(Person person) {
        Person[] coauthors = emptyPersArray;
        if (person != null) {
            coauthors = graph.get(person);
            if (coauthors == null) coauthors = createCouthorsArray(person);
        }
        return Arrays.stream(coauthors);
    }

    @Override
    public int numberOfCoauthors(Person person) {
        if (person == null) return 0;
        Person[] coauthors = graph.get(person);
        if (coauthors == null) coauthors = createCouthorsArray(person);
        return coauthors.length;
    }

    @Override
    public boolean hasCoauthors(Person first, Person second) {
        if (first == null || second == null) return false;
        Person[] coauths1 = graph.get(first);
        Person[] coauths2 = graph.get(second);
        return Arrays.asList(coauths1).contains(second) || Arrays.asList(coauths2).contains(first);
    }

    /**
     * Retrieve the number of coauthor edges contained in this coauthor graph.
     *
     * @return The number of edges.
     */
    public int getNumberOfEdges() {
        return this.edges;
    }

    @Override
    public int numberOfCoauthorCommunities(Person person) throws NullPointerException {
        LocalCoauthorsGraph local;
        if (!this.localMap.containsKey(person)) {
            local = new LocalCoauthorsGraph(this, person);
            this.localMap.put(person, local);
        }
        else {
            local = this.localMap.get(person);
        }
        return local.numberOfCommunities();
    }

    @Override
    public Collection<Person> getCoauthorCommunity(Person person, int index)
            throws NullPointerException, IndexOutOfBoundsException {
        LocalCoauthorsGraph local;
        if (!this.localMap.containsKey(person)) {
            local = new LocalCoauthorsGraph(this, person);
            this.localMap.put(person, local);
        }
        else {
            local = this.localMap.get(person);
        }
        return local.getCommunity(index);
    }

    @Override
    public LocalCoauthorsGraph getCoauthorGraph(Person person) throws NullPointerException {
        LocalCoauthorsGraph local;
        if (!this.localMap.containsKey(person)) {
            local = new LocalCoauthorsGraph(this, person);
            this.localMap.put(person, local);
        }
        else {
            local = this.localMap.get(person);
        }
        return local;
    }

    // TODO: testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws IOException, SAXException {

        if (args.length < 1) {
            System.out.format("Usage: %s <dblp.xml>\n", SimpleLazyCoauthorGraph.class.getSimpleName());
            System.exit(0);
        }
        SimpleCoauthorGraph dblp = new SimpleCoauthorGraph(args[0]);

        System.out.println("dblp.numberOfPublications() = " + dblp.numberOfPublications());
        System.out.println("dblp.numberOfPersons() = " + dblp.numberOfPersons());
        System.out.println("dblp.getNumberOfEdges() = " + dblp.getNumberOfEdges());
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
            System.out.print("dblp.getCoauthors(" + key + ") = [");
            for (Person coauth : dblp.getCoauthors(pers)) {
                if (coauth == null) System.out.print("null");
                else System.out.print(coauth.getPrimaryName().name());
                System.out.print(",");
            }
            System.out.println("]");
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
            System.out.print("dblp.getCoauthors(" + peep + ") = [");
            for (Person coauth : dblp.getCoauthors(pers)) {
                if (coauth == null) System.out.print("null");
                else System.out.print(coauth.getPrimaryName().name());
                System.out.print(",");
            }
            System.out.println("]");
            System.out.println("dblp.numberOfCoauthors(" + peep + ") = "
                    + dblp.numberOfCoauthors(pers));
            System.out.println("dblp.numberOfCoauthorCommunities(" + peep + ") = "
                    + dblp.numberOfCoauthorCommunities(pers));
            System.out.println();
        }
        System.out.println("max number of coauthors = "
                + dblp.persons().mapToInt(p -> dblp.numberOfCoauthors(p)).max().getAsInt());
        System.out.println("#coauthors: #persons");
        //@formatter:off
        Map<Integer, Long> stats = dblp.persons()
                .collect(Collectors.groupingBy(pers -> dblp.numberOfCoauthors(pers), Collectors.counting()));
        stats.entrySet().stream()
        .sorted((Map.Entry<Integer, Long> entry1, Map.Entry<Integer, Long> entry2) -> entry1.getKey().compareTo(entry2.getKey()))
        .forEachOrdered(entry -> System.out.println(entry.getKey().toString() + ": " + entry.getValue().toString() + "x"));
        //@formatter:on
    }

}
