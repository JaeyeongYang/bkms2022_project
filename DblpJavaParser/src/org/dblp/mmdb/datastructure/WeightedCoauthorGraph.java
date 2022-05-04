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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dblp.mmdb.Mmdb;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
@Deprecated
public class WeightedCoauthorGraph extends AbstractCoauthorGraph {

    /** The weighted adjacency list of this graph as a map of maps. */
    private final Map<Person, Map<Person, Long>> graph;
    /** The map of all local coauthor graphs. */
    private final Map<Person, LocalCoauthorsGraph> localMap;
    /** The number of edges in this graph. */
    private int edges;
    /** Whether this graph is completely build. */
    private boolean completed;
    // ???
    private static final Map<Person, Long> emptyCoMap = new HashMap<Person, Long>();

    public WeightedCoauthorGraph(Mmdb mmdb) {
        super(mmdb);
        graph = new ConcurrentHashMap<Person, Map<Person, Long>>();
        this.localMap = new ConcurrentHashMap<>();
        edges = 0;
        completed = false;
    }

    private Map<Person, Long> buildCoauthorsMap(Person person) {
        Map<Person, Long> coauthors = collectWeightedCoauthors(person);
        graph.put(person, coauthors);
        return coauthors;
    }

    public Map<Person, Long> getWeightedCoauthors(Person person) {
        if (person == null) return null;
        Map<Person, Long> coauthors = graph.get(person);
        if (coauthors == null) coauthors = buildCoauthorsMap(person);
        return Collections.unmodifiableMap(coauthors);
    }

    @Override
    public Collection<Person> getCoauthors(Person person) {
        return getWeightedCoauthors(person).keySet();
    }

    @Override
    public int numberOfCoauthors(Person person) throws NullPointerException {
        return this.graph.get(person).size();
    }

    @Override
    public Stream<Person> coauthors(Person person) {
        Map<Person, Long> coauthors = emptyCoMap;
        if (person != null) {
            coauthors = graph.get(person);
            if (coauthors == null) coauthors = buildCoauthorsMap(person);
        }
        return coauthors.keySet().stream();
    }

    public int getNumberOfCoauthors(Person person) {
        if (person == null) return 0;
        Map<Person, Long> coauthors = graph.get(person);
        if (coauthors == null) coauthors = buildCoauthorsMap(person);
        return coauthors.size();
    }

    public static Map<Person, Long> collectWeightedCoauthors(Person person) {
        Map<Person, Long> coauths =
                person.publications().flatMap(Publication::names).map(PersonName::getPerson).collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        coauths.remove(person);
        return coauths;
    }

    public int getNumberOfEdges() {
        /*
         * force the construction of the complete graph
         */
        if (!completed) {
            edges = mmdb.persons().parallel().mapToInt(p -> getNumberOfCoauthors(p)).sum() / 2;
            completed = true;
        }
        return edges;
    }

    @Override
    public boolean hasCoauthors(Person first, Person second) throws NullPointerException {
        Set<Person> coauths1 = graph.get(first).keySet();
        Set<Person> coauths2 = graph.get(second).keySet();
        return coauths1.contains(second) || coauths2.contains(first);
    }

    /**
     * Retrieves the frequency two given persons have coauthored in dblp.
     *
     * @param first The first person.
     * @param second The second person.
     * @return The frequency.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    public long coauthorFrequency(Person first, Person second) throws NullPointerException {
        if (first == null || second == null) throw new NullPointerException();
        try {
            return graph.get(first).get(second);
        }
        catch (NullPointerException ex1) {
            try {
                return graph.get(second).get(first);
            }
            catch (NullPointerException ex2) {
                return 0;
            }
        }
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

    public static void main(String[] args) throws IOException, SAXException {
        if (args.length < 1) {
            System.out.println("Usage: java org/dblp/basics/WeightedCoauthorGraph [dblp.xml]");
            System.exit(0);
        }
        Mmdb dblp = new Mmdb(args[0], true);

        WeightedCoauthorGraph coauthorgraph = new WeightedCoauthorGraph(dblp);

        long start = System.currentTimeMillis();
        System.out.println("coauthor graph edges : " + coauthorgraph.getNumberOfEdges());
        long end = System.currentTimeMillis();
        System.out.println("-- coauthor graph build time: " + (end - start));

        Person kurt = dblp.getPersonName("Kurt Mehlhorn").getPerson();

        System.out.println("#coauthors of Kurt Mehlhorn = "
                + coauthorgraph.getNumberOfCoauthors(kurt));
        coauthorgraph.getWeightedCoauthors(kurt).entrySet().stream().sorted((
                Map.Entry<Person, Long> e1,
                Map.Entry<Person, Long> e2) -> e1.getValue().compareTo(e2.getValue())).forEachOrdered(e -> System.out.println(e.getKey().nameAt(0).name()
                        + " : " + e.getValue().toString()));

        System.out.println("coauthor graph edges : " + coauthorgraph.getNumberOfEdges());
        System.out.println("max number of coauthors : "
                + dblp.getPersons().stream().mapToInt(p -> coauthorgraph.getNumberOfCoauthors(p)).max().getAsInt());

        System.out.println("#coauthors of Manfred Jackel = "
                + coauthorgraph.getNumberOfCoauthors(dblp.getPersonName("Manfred Jackel").getPerson()));
        System.out.println("coauthor graph edges : " + coauthorgraph.getNumberOfEdges());
        System.out.println("\n#coauthors / #persons");
        Map<Integer, Long> s =
                dblp.getPersons().stream().collect(Collectors.groupingBy(p -> coauthorgraph.getNumberOfCoauthors(p), Collectors.counting()));

        s.entrySet().stream().sorted((Map.Entry<Integer, Long> e1,
                Map.Entry<Integer, Long> e2) -> e1.getKey().compareTo(e2.getKey())).forEachOrdered(e -> System.out.println(e.getKey().toString()
                        + " : " + e.getValue().toString()));
    }
}
