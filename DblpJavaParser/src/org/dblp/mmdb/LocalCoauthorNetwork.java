package org.dblp.mmdb;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.dblp.util.Tally;
import org.xml.sax.SAXException;


/**
 * A class representing the 1-hop coauthor neighborhood of a given reference person. The nodes of
 * the neighborhood are clustered into connected components according to their coauthor relation.
 *
 * @author mra
 * @version 2014-08-01
 */
public class LocalCoauthorNetwork {

    /** The dblp coauthor graph. */
    protected final AbstractCoauthorGraph dblp;
    /** The center person of this coauthor graph. */
    private final Person center;
    /** Whether disambiguation odes should connect the network. */
    private final boolean allowDisamb;
    /** The coauthors of the center person. */
    private final Person[] coauthors;
    /**
     * The index array of the proxy representatives. That is, the local cluster of coauthor
     * {@code coauthors[i]} is represented by coauthor {@code coauthors[proxy[i]]}.
     */
    private final int[] proxy;
    /**
     * The index array of the cluster number. That is, the coauthor {@code coauthors[i]} is part of
     * cluster {@code cluster[i]}.
     */
    private final int[] cluster;
    /** The total number of coauthor communities. */
    private final int numberOfClusters;
    /**
     * The size array of the clusters. That is, the cluster of index {@code k} has
     * {@code clusterSize[k]} members.
     */
    private final int[] clusterSize;
    /** The entropy score induced to the center person by its coauthors. */
    private double entropy;

    /**
     * Static, temporary tally used to build the clusters and order them according to the number of
     * their members when building the initial clustering.
     */
    // FIXME: decouple from Tally utility class
    private static Tally<Integer> tmpClusterTally = new Tally<>(100);

    /**
     * Creates the local coauthor graph of the 1-hop neighborhood for the given reference center.
     *
     * @param dblp The dblp coauthor graph.
     * @param center The reference center.
     * @param allowDisambiguations If {@code true}, disambiguation profiles are valid coauthor
     *            nodes. Otherwise, disambiguation profiles are considered to have no coauthors.
     */
    public LocalCoauthorNetwork(AbstractCoauthorGraph dblp, Person center,
            boolean allowDisambiguations) {
        this.dblp = dblp;
        this.center = center;
        this.allowDisamb = allowDisambiguations;

        Collection<Person> tmpPersons = dblp.getCoauthors(center);
        this.coauthors = tmpPersons.toArray(new Person[tmpPersons.size()]);

        // start with each person pointing to itself as proxy
        this.proxy = new int[this.coauthors.length];
        for (int i = 0; i < this.coauthors.length; i++) {
            this.proxy[i] = i;
        }
        // for all pairs of persons, join proxies if they are coauthors
        for (int i = 1; i < this.coauthors.length; i++) {
            if (!allowDisambiguations && this.coauthors[i].isDisambiguation()) continue;
            for (int j = 0; j < i; j++) {
                if (!allowDisambiguations && this.coauthors[j].isDisambiguation()) continue;
                if (dblp.hasCoauthors(this.coauthors[i], this.coauthors[j])) {
                    unionClusterOf(i, j);
                }
            }
        }
        synchronized (LocalCoauthorNetwork.tmpClusterTally) {
            // tally the occurrences of the proxies
            tmpClusterTally.clear();
            for (int i = 0; i < this.coauthors.length; i++) {
                tmpClusterTally.add(findProxyOf(i));
            }
            // get sorted list of proxies
            List<Integer> clusterProxies = tmpClusterTally.getTop();
            // build clustering
            this.cluster = new int[this.coauthors.length];
            this.numberOfClusters = clusterProxies.size();
            this.clusterSize = new int[this.numberOfClusters];
            for (int k = 0; k < this.numberOfClusters; k++) {
                this.clusterSize[k] = 0;
                int currentClusterProxy = clusterProxies.get(k);
                for (int i = 0; i < this.coauthors.length; i++) {
                    if (findProxyOf(i) == currentClusterProxy) {
                        this.cluster[i] = k;
                        this.clusterSize[k] += 1;
                    }
                }
            }
            // lazy init entropy
            this.entropy = -1.0;
        }
    }

    /**
     * Find the proxy of coauthor with index {@code i} using Tarjan's union-find principle. The path
     * to the ultimate proxy is collapsed while traversing through the proxy-tree, thereby speeding
     * up future queries.
     *
     * @param i The index of the query coauthor.
     * @return The index of the proxy representative.
     */
    private int findProxyOf(int i) {
        if (this.proxy[i] != i) this.proxy[i] = findProxyOf(this.proxy[i]);
        return this.proxy[i];
    }

    /**
     * Union the clusters of the both coauthors given by their index using Tarjan's union-find
     * principle. The new cluster proxy is determined randomly among the two initial proxies.
     *
     * @param i The index of the first coauthor.
     * @param j The index of the second coauthor.
     */
    private void unionClusterOf(int i, int j) {
        int proxy_i = findProxyOf(i);
        int proxy_j = findProxyOf(j);

        if (proxy_i != proxy_j) {
            if ((i + j) % 2 == 0) this.proxy[proxy_j] = proxy_i;
            else this.proxy[proxy_i] = proxy_j;
        }
    }

    /**
     * Retrieve the center person of this local coauthor graph.
     *
     * @return The center person.
     */
    public Person getCenter() {
        return this.center;
    }

    /**
     * Retrieve the 1-hop coauthor neighborhood of the (and excluding) the center person of this
     * local coauthor graph.
     *
     * @return The 1-hop coauthor neighborhood.
     */
    public Collection<Person> getNeighborhood() {
        return Collections.unmodifiableList(Arrays.asList(this.coauthors));
    }

    /**
     * Retrieve the coauthor community of the given index.
     *
     * @param index The community index.
     * @return The coauthors of this community.
     * @throws IndexOutOfBoundsException if the index is out of range
     *             {@code (index < 0 || index >= getNumberOfCommunities())}
     */
    public Collection<Person> getCommunity(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= this.numberOfClusters) throw new IndexOutOfBoundsException();

        List<Person> community = new ArrayList<>(this.clusterSize[index]);
        for (int i = 0; i < this.coauthors.length; i++) {
            if (this.cluster[i] == index) community.add(this.coauthors[i]);
        }
        return Collections.unmodifiableList(community);
    }

    /**
     * Retrieve the community index of the given coauthor.
     *
     * @param person The coauthors to look up.
     * @return index The community index between {@code 0} and {@code getNumberOfCommunities())}, or
     *         {@code -1} if no such coauthor exists.
     */
    public int getIndex(Person person) {
        for (int i = 0; i < this.coauthors.length; i++) {
            if (person == this.coauthors[i]) return this.cluster[i];
        }
        return -1;
    }

    /**
     * Return the number of neighboring coauthors (excluding the center person) in this local
     * coauthor graph.
     *
     * @return The number of coauthors.
     */
    public int numberOfNeighbors() {
        return this.coauthors.length;
    }

    /**
     * Return the number of coauthor communities in this local coauthor graph.
     *
     * @return The number of communities.
     */
    public int numberOfCommunities() {
        return this.numberOfClusters;
    }

    /**
     * Get the entropy score induced to the central person by its local coauthor graph. This value
     * is the relative Shannon entropy {@code H } of the distribution induced by the coauthor
     * clustering. That is, for {@code k} clusters of sizes {@code n1,...,nk} and
     * {@code N = n1 + ... + nk} we have
     * <pre>H = ( n1/N * log(N/n1) + ... + nk/N * log(N/nk) ) / log(k)</pre>
     *
     * @return The entropy score.
     * @see <a href="https://en.wikipedia.org/wiki/Entropy_(information_theory)">Wikipedia</a>
     */
    public double getEntropy() {

        if (this.entropy < 0.0) {
            if (this.numberOfClusters < 2) {
                this.entropy = 0.0;
            }
            else {
                double score = 0.0;
                double total = this.coauthors.length;
                for (int k = 0; k < this.numberOfClusters; k++) {
                    double prob = this.clusterSize[k] / total;
                    double info = -Math.log(prob);
                    score += prob * info;
                }
                this.entropy = score / Math.log(this.numberOfClusters);
            }
        }
        return this.entropy;
    }

    /**
     * Print a textual cluster description of this local coauthor network to the given print stream.
     * Used mainly for debugging.
     *
     * @param out The output stream.
     */
    void printClusterDescription(PrintStream out) {

        out.println(String.format("%s (%d)", this.center.getPrimaryName().name(), this.cluster.length));
        for (int k = 0; k < this.numberOfClusters; k++) {
            out.print(String.format("+-- Cluster '%d': ", k));
            for (int i = 0; i < this.coauthors.length; i++) {
                if (this.cluster[i] == k) {
                    out.format("%s [%d] ", this.coauthors[i].getPrimaryName().name(), findProxyOf(i));
                }
            }
            out.println();
        }
        boolean first = true;
        for (int i = 0; i < this.coauthors.length; i++) {
            if (!this.allowDisamb && this.coauthors[i].isDisambiguation()) continue;
            for (int j = 0; j < this.coauthors.length; j++) {
                if (i == j || this.cluster[i] == this.cluster[j]) continue;
                if (!this.allowDisamb && this.coauthors[j].isDisambiguation()) continue;
                if (this.dblp.hasCoauthors(this.coauthors[i], this.coauthors[j])) {
                    if (first) {
                        out.println("!!! unexpected iter-cluster edges:");
                        first = false;
                    }
                    out.format("! %s (%d) --- %s (%d)\n", this.coauthors[i].getPrimaryName().name(), this.cluster[i], this.coauthors[j].getPrimaryName().name(), this.cluster[j]);
                }
            }
        }
    }

    /**
     * Print a textual cluster description of this local coauthor network to {@code stdout}. Used
     * mainly for debugging.
     */
    public void printClusterDescription() {
        printClusterDescription(System.out);
    }

    // TODO: testing
    @SuppressWarnings("javadoc")
    public static void main(String[] args) throws NullPointerException, IOException, SAXException {

        if (args.length < 2) {
            System.out.format("Usage: %s <dblp.xml> <dblp.dtd>\n", LocalCoauthorNetwork.class.getSimpleName());
            System.exit(0);
        }

        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        RecordDbInterface dblp;
        if (dblpXmlFilename.toLowerCase().endsWith(".gz")) {
            dblp = RecordDb.fromGzip(dblpXmlFilename, dblpDtdFilename, true);
        }
        else dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, true);

        String[] keys = { "homepages/w/WeiWang11", "homepages/w/AviWigderson", "homepages/a/NAlon",
                "homepages/p/CHPapadimitriou", "homepages/22/3485" };

        System.out.println("=== Disambiguations allowed ===");
        for (String key : keys) {
            Person pers = dblp.getPerson(key);
            if (pers == null) continue;
            dblp.getCoauthorNetwork(pers, true).printClusterDescription();
        }

        System.out.println("=== Disambiguations not allowed ===");
        for (String key : keys) {
            Person pers = dblp.getPerson(key);
            if (pers == null) continue;
            dblp.getCoauthorNetwork(pers, false).printClusterDescription();
        }
    }
}
