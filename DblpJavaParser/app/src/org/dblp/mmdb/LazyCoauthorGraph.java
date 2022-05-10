package org.dblp.mmdb;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.xml.sax.SAXException;


/**
 * A weighted coauthor graph implementation. The weights of this graph represent the multiplicity of
 * the coauthorship of two authors.
 * <p>
 * This graph is loaded lazy into main memory whenever any local information is queried.
 */
class LazyCoauthorGraph extends AbstractCoauthorGraph {

    /** The weighted adjacency list of this graph as a map. */
    private final Map<Person, Map<Person, Long>> coauthors;
    /**
     * The map of all local coauthor neighborhood graphs, using disambiguation profiles as normal
     * nodes.
     */
    private final Map<Person, LocalCoauthorNetwork> networksAllowed;
    /**
     * The map of all local coauthor neighborhood graphs, where disambiguation profiles do not link
     * the network.
     */
    private final Map<Person, LocalCoauthorNetwork> networksWithout;
    /** A counter for logging progress of the lazy initialization of persons. */
    private int coauthorCount;
    /** A counter for logging progress of the lazy initialization of local networks. */
    private int networkCount;
    /** The increment used for logging the progress. */
    private static final int COUNTER_INCREMENT = 100000;

    /**
     * Create a new coauthor graph from the given MMDB. The local parts of the actual coauthors are
     * not materialized before any required information is queried.
     *
     * @param mmdb The MMDB.
     */
    LazyCoauthorGraph(MmdbInterface mmdb) {
        super(mmdb);
        this.coauthors = new HashMap<>();
        this.networksAllowed = new HashMap<>();
        this.networksWithout = new HashMap<>();
        this.coauthorCount = COUNTER_INCREMENT;
        this.networkCount = COUNTER_INCREMENT;
    }

    /**
     * Lazy create the given persons' adjacent coauthors in this graph.
     *
     * @param persons The center persons.
     */
    private void ensureCoauthors(Person... persons) {
        for (Person person : persons) {
            if (!this.coauthors.containsKey(person)) {
                Map<Person, Long> coauthCount =
                        person.publications().parallel().flatMap(Publication::names).map(PersonName::getPerson).collect(Collectors.groupingBy(x -> x, Collectors.counting()));
                coauthCount.remove(person);
                this.coauthors.put(person, coauthCount);
                while (this.coauthors.size() >= this.coauthorCount) {
                    Mmdb.LOG.info("lazy building coauthor graph", this.coauthorCount
                            + " person nodes ...");
                    this.coauthorCount += COUNTER_INCREMENT;
                }
            }
        }
    }

    /**
     * Lazy create the given persons' local coauthor network.
     *
     * @param persons The persons.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor nodes
     *            that link the network. Otherwise, disambiguation profiles are considered to have
     *            no coauthors and end up as singleton cluster nodes in the network.
     */
    private void ensureLocalNetwork(boolean allowDisambiguations, Person... persons) {
        ensureCoauthors(persons);
        Map<Person, LocalCoauthorNetwork> networks =
                allowDisambiguations ? this.networksAllowed : this.networksWithout;
        for (Person person : persons) {
            if (!networks.containsKey(person)) {
                LocalCoauthorNetwork local =
                        new LocalCoauthorNetwork(this, person, allowDisambiguations);
                networks.put(person, local);
                while (networks.size() >= this.networkCount) {
                    Mmdb.LOG.info("lazy building coauthor graph", this.networkCount
                            + " local networks ...");
                    this.networkCount += COUNTER_INCREMENT;
                }
            }
        }
    }

    /**
     * Trigger that all local structures are build for the given persons, if they have not been
     * build yet.
     */
    void ensurePerson(Person... persons) {
        ensureCoauthors(persons);
        ensureLocalNetwork(true, persons);
        ensureLocalNetwork(false, persons);
    }

    @Override
    Collection<Person> getCoauthors(Person person) throws NullPointerException {
        ensureCoauthors(person);
        return Collections.unmodifiableSet(this.coauthors.get(person).keySet());
    }

    @Override
    Stream<Person> coauthors(Person person) throws NullPointerException {
        ensureCoauthors(person);
        return this.coauthors.get(person).keySet().stream();
    }

    @Override
    int numberOfCoauthors(Person person) throws NullPointerException {
        ensureCoauthors(person);
        return this.coauthors.get(person).size();
    }

    @Override
    boolean hasCoauthors(Person first, Person second) throws NullPointerException {
        if (first == null || second == null) throw new NullPointerException();
        ensureCoauthors(first, second);
        return this.coauthors.get(first).containsKey(second)
                || this.coauthors.get(second).containsKey(first);
    }

    /**
     * Retrieve the multiplicity of the coauthorship of two given authors.
     * <p>
     * A weight of {@code 0.0} is returned if, and only if, {@link #hasCoauthors(Person, Person)}
     * returns {@code false}. The weight returned by this method is symmetric, i.e.:<pre>
     * weight(x, y) == weight(y, x)
     * </pre>
     *
     * @param first The first person.
     * @param second The second person.
     * @return The weight.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    @Override
    double weight(Person first, Person second) throws NullPointerException {
        if (first == null || second == null) throw new NullPointerException();
        ensureCoauthors(first);
        if (this.coauthors.get(first).containsKey(second))
            return this.coauthors.get(first).get(second);
        return 0.0;
    }

    @Override
    LocalCoauthorNetwork getCoauthorGraph(Person person, boolean allowDisambiguations)
            throws NullPointerException {
        if (person == null) throw new NullPointerException();
        ensureLocalNetwork(allowDisambiguations, person);
        if (allowDisambiguations) return this.networksAllowed.get(person);
        return this.networksWithout.get(person);
    }

    // TODO: testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws IOException, SAXException {

        if (args.length < 2) {
            System.out.format("Usage: %s <dblp.xml> <dblp.dtd>\n", LazyCoauthorGraph.class.getSimpleName());
            System.exit(0);
        }

        MmdbInterface dblp = new Mmdb(args[0], args[1], false);

        System.out.println("dblp.numberOfPublications() = " + dblp.numberOfPublications());
        System.out.println("dblp.numberOfPersons() = " + dblp.numberOfPersons());
        System.out.println();

        LazyCoauthorGraph graph = new LazyCoauthorGraph(dblp);
        Person avi = dblp.getPerson("homepages/w/AviWigderson");
        Person noga = dblp.getPerson("homepages/a/NAlon");
        Person cristos = dblp.getPerson("homepages/p/CHPapadimitriou");

        System.out.println("graph.numberOfCoauthors(avi) = " + graph.numberOfCoauthors(avi));
        System.out.println("graph.hasCoauthors(avi,noga) = " + graph.hasCoauthors(avi, noga));
        System.out.println("graph.weight(avi,noga) = " + graph.weight(avi, noga));
        System.out.println("graph.hasCoauthors(avi,cristos) = " + graph.hasCoauthors(avi, cristos));
        System.out.println("graph.weight(avi,cristos) = " + graph.weight(avi, cristos));
        System.out.println();

        Person top = graph.coauthors(noga).parallel().max((c1,
                c2) -> Double.compare(graph.weight(noga, c1), graph.weight(noga, c2))).orElse(null);
        System.out.format("argmax weight(noga,x) = %s (%d)\n", top.getPrimaryName().name(), (int) graph.weight(noga, top));

        top = graph.coauthors(noga).parallel().max((c1,
                c2) -> Double.compare(graph.weight(c1, noga), graph.weight(c2, noga))).orElse(null);
        System.out.format("argmax weight(x,noga) = %s (%d)\n", top.getPrimaryName().name(), (int) graph.weight(top, noga));
        System.out.println();

        System.out.println(graph.shortestPath(noga, cristos, true).stream().map(Person::getPrimaryName).map(PersonName::name).collect(Collectors.joining(", ", "[", "]")));
    }
}
