package org.dblp.mmdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Stream;


/**
 * Provides default implementations for an MMDB based coauthor graph.
 *
 * @author mra
 */
abstract class AbstractCoauthorGraph {

    /** An empty coauthor graph. Used for testing code without building an actual graph object. */
    static final AbstractCoauthorGraph EMTPY_COAUTHOR_GRAPH =
            new LazyCoauthorGraph(Mmdb.EMTPY_MMDB);

    /**
     * Computes the minimum edge path between two dblp authors in the coauthor graph, using a
     * variant of the classic breadth-first algorithm.
     */
    protected class MinimumEdgePathAlgorithm {

        /** The source person. */
        protected final Person source;
        /** The source destination. */
        protected final Person destination;
        /** Whether disambiguation profiles are valid mid-path nodes. */
        protected boolean allowDisambiguations;
        /** The predecessor map. */
        protected final Map<Person, Person> preMap;
        /** The distance map. */
        protected final Map<Person, Double> distMap;
        /** The shortest path. */
        protected List<Person> path;
        /** The shortest path distance. */
        protected Double distance;

        /**
         * Compute a shortest path.
         *
         * @param source The source person
         * @param destination The source destination
         * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
         *            nodes that link the network. Otherwise, disambiguation profiles are considered
         *            to have no outgoing coauthors.
         */
        MinimumEdgePathAlgorithm(Person source, Person destination, boolean allowDisambiguations) {
            this.source = source;
            this.destination = destination;
            this.allowDisambiguations = allowDisambiguations;
            this.distMap = new HashMap<>();
            this.preMap = new HashMap<>();
            run();
            this.distMap.clear();
            this.preMap.clear();
        }

        /**
         * Compute a shortest path. Disambiguation profiles are considered to be valid coauthor
         * nodes that link the network.
         *
         * @param source The source person
         * @param destination The source destination
         */
        MinimumEdgePathAlgorithm(Person source, Person destination) {
            this(source, destination, true);
        }

        /** Compute a new shortest path. */
        protected void run() {
            Set<Person> done = new HashSet<>();
            PriorityQueue<Person> todo = new PriorityQueue<>((p, q) -> compareDist(p, q));

            this.distMap.put(this.source, 0.0);
            todo.add(this.source);
            while (!todo.isEmpty()) {
                // retire current
                Person current = todo.remove();
                done.add(current);
                todo.remove(current);
                if (current == this.destination) break;
                // find minimal distances
                if (!this.allowDisambiguations && current.isDisambiguation()) continue;
                Collection<Person> adjacentNodes = getCoauthors(current);
                for (Person next : adjacentNodes) {
                    if (done.contains(next)) continue;
                    double newDist = getShortestDistance(current) + edgeDist(current, next);
                    if (getShortestDistance(next) > newDist) {
                        this.distMap.put(next, newDist);
                        this.preMap.put(next, current);
                        todo.add(next);
                    }
                }
            }
            // store distance & path
            this.distance = getShortestDistance(this.destination);
            List<Person> foundPath = new LinkedList<>();
            Person step = this.destination;
            if (this.preMap.get(step) == null) this.path = Collections.emptyList();
            foundPath.add(step);
            while (this.preMap.get(step) != null) {
                step = this.preMap.get(step);
                foundPath.add(step);
            }
            Collections.reverse(foundPath);
            this.path = foundPath;
        }

        /**
         * Compare two persons' distances.
         *
         * @param p1 The first person.
         * @param p2 The second person.
         * @return {@code [-1, 0, 1]} iff {@code [<, =, >]}, respectively.
         */
        protected int compareDist(Person p1, Person p2) {
            if (this.distMap.get(p1) < this.distMap.get(p2)) return -1;
            if (this.distMap.get(p1) > this.distMap.get(p2)) return 1;
            return 0;
        }

        /**
         * Returns {@code 1.0} if two persons share an edge, otherwise returns
         * {@link Double#POSITIVE_INFINITY}.
         *
         * @param p1 The first person.
         * @param p2 The second person.
         * @return The edge distance.
         */
        protected double edgeDist(Person p1, Person p2) {
            if (hasCoauthors(p1, p2)) return 1.0;
            return Double.POSITIVE_INFINITY;
        }

        /**
         * Retrieve the shortest distance from the source to a given person.
         *
         * @param pers The person.
         * @return The distance.
         */
        protected double getShortestDistance(Person pers) {
            Double dist = this.distMap.get(pers);
            if (dist == null) return Double.POSITIVE_INFINITY;
            return dist;
        }

        /**
         * Retrieve the distance between source and destination. If no path exists then this method
         * will return {@link Double#POSITIVE_INFINITY}.
         *
         * @return The distance.
         */
        Double getDistance() {
            return this.distance;
        }

        /**
         * Retrieve the path between source and destination. If no path exists then this method will
         * return and empty list.
         *
         * @return The path.
         */
        List<Person> getPath() {
            return this.path;
        }

    }

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
     * Retrieves an unmodifiable list view of all coauthors of the given person.
     *
     * @param person The person.
     * @return The coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    abstract Collection<Person> getCoauthors(Person person) throws NullPointerException;

    /**
     * Retrieves a sequential stream of all coauthors of the given person.
     *
     * @param person The person.
     * @return The stream of coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    abstract Stream<Person> coauthors(Person person) throws NullPointerException;

    /**
     * Retrieve the number of coauthors of the given person.
     *
     * @param person The person.
     * @return The number of coauthors.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    abstract int numberOfCoauthors(Person person) throws NullPointerException;

    /**
     * Checks whether the given persons are coauthors in dblp.
     * <p>
     * The result of this method is expected to be symmetric, i.e. <pre>
     * hasCoauthors(x, y) == hasCoauthors(y, x)
     * </pre>
     *
     * @param first The first person.
     * @param second The second person.
     * @return {@code true} if both persons are coauthors, otherwise {@code false}.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    abstract boolean hasCoauthors(Person first, Person second) throws NullPointerException;

    /**
     * Retrieve the weight of the coauthor relation between the two given persons.
     * <p>
     * <strong>Note:</strong> The interpretation of the weight depends on the actual implementation
     * of the weighted graph. Two persons may have a non-trivial weight even if
     * {@link #hasCoauthors(Person, Person)} returns {@code false}, and the returned weight <em>does
     * not</em> necessarily have to be symmetric.
     *
     * @param first The first person.
     * @param second The second person.
     * @return The weight.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    abstract double weight(Person first, Person second) throws NullPointerException;

    /**
     * Retrieves a list of person that form path between the two authors using a minimal number of
     * edges. If no such path exists, an empty list is returned.
     *
     * @param first The first person.
     * @param second The second person.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
     *            nodes. Otherwise, disambiguation profiles are considered to have no coauthors.
     * @return The path as list of persons, or an empty list if no such path exists.
     * @throws NullPointerException if either of the given persons is {@code null}.
     */
    List<Person> shortestPath(Person first, Person second, boolean allowDisambiguations)
            throws NullPointerException {
        if (first == null || second == null) throw new NullPointerException();

        MinimumEdgePathAlgorithm min =
                new MinimumEdgePathAlgorithm(first, second, allowDisambiguations);
        return min.getPath();
    }

    /**
     * Retrieve the local coauthor graph of a given person.
     *
     * @param person The person.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
     *            nodes. Otherwise, disambiguation profiles are considered to have no coauthors.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    abstract LocalCoauthorNetwork getCoauthorGraph(Person person, boolean allowDisambiguations)
            throws NullPointerException;

    /**
     * Return the number of coauthor communities of the given person.
     *
     * @param person The person.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
     *            nodes. Otherwise, disambiguation profiles are considered to have no coauthors.
     * @return The number of communities.
     * @throws NullPointerException if the specified person is {@code null}.
     */
    int numberOfCoauthorCommunities(Person person, boolean allowDisambiguations)
            throws NullPointerException {
        if (person == null) throw new NullPointerException();
        return getCoauthorGraph(person, allowDisambiguations).numberOfCommunities();
    }

    /**
     * Retrieve a specific coauthor community of a given person.
     *
     * @param person The person.
     * @param index The community index.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
     *            nodes. Otherwise, disambiguation profiles are considered to have no coauthors.
     * @return The coauthors of this community.
     * @throws NullPointerException if the specified person is {@code null}.
     * @throws IndexOutOfBoundsException if the index is out of range
     *             <code>(index < 0 || index >= getNumberOfCommunities())</code>
     */
    Collection<Person> getCoauthorCommunity(Person person, int index, boolean allowDisambiguations)
            throws NullPointerException, IndexOutOfBoundsException {
        return getCoauthorGraph(person, allowDisambiguations).getCommunity(index);
    }

    /**
     * Retrieve the community index of a coauthor of a given person.
     *
     * @param person The central person.
     * @param coauthor The coauthor to look up in the coauthor communities of {@code person}.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
     *            nodes. Otherwise, disambiguation profiles are considered to have no coauthors.
     * @return The community index between {@code 0} and {@code getNumberOfCommunities())}, or
     *         {@code -1} if no such coauthor exists.
     * @throws NullPointerException if one of the specified persons is {@code null}.
     */
    int getCoauthorCommunityIndex(Person person, Person coauthor, boolean allowDisambiguations)
            throws NullPointerException {
        if (person == null || coauthor == null) throw new NullPointerException();
        return getCoauthorGraph(person, allowDisambiguations).getIndex(coauthor);
    }

}
