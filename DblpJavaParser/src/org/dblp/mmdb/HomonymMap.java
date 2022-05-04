package org.dblp.mmdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * A data structure for mapping homonym base names to persons.
 *
 * @author mra
 */
class HomonymMap {

    /** The homonym number pattern of a dblp name. */
    static private final Pattern HOM_PATTERN = Pattern.compile(".* [0-9][0-9][0-9][0-9]");

    /** The MMDB. */
    @SuppressWarnings("unused")
    private final MmdbInterface mmdb;
    /** The base-name-string-to-person-name-set map. */
    private final Map<String, List<PersonName>> homMap;

    /**
     * Create a new homonym map data structure from the given MMDB.
     *
     * @param mmdb The MMDB.
     */
    HomonymMap(MmdbInterface mmdb) {

        this.mmdb = mmdb;

        // init map
        long start = System.currentTimeMillis();
        this.homMap = new TreeMap<>();

        // collect homonymous person names
        for (PersonName persName : mmdb.getPersonNames()) {
            String baseName = persName.name(false);
            List<PersonName> homList;
            if (this.homMap.containsKey(baseName)) {
                homList = this.homMap.get(baseName);
            }
            else {
                homList = new ArrayList<>(1);
                this.homMap.put(baseName, homList);
            }
            homList.add(persName);
        }
        long end = System.currentTimeMillis();
        Mmdb.LOG.info("build homonym map time", (end - start) + " ms");
    }

    /**
     * Retrieves an unmodifiable Collection view of the homonyms of the given person name in the
     * MMDB.
     *
     * @param name The name.
     * @return The homonyms, or an empty set if the name is not in dblp.
     */
    Collection<PersonName> getHomonyms(String name) {
        String baseName = stripHomonymNumber(name);
        if (this.homMap.containsKey(baseName))
            return Collections.unmodifiableCollection(this.homMap.get(baseName));
        return Collections.emptySet();
    }

    /**
     * Retrieves a sequential stream of the homonyms of the given person name in the MMDB.
     *
     * @param name The name.
     * @return The stream of homonyms, or an empty stream if the name is not in dblp.
     */
    Stream<PersonName> homonyms(String name) {
        String baseName = stripHomonymNumber(name);
        if (this.homMap.containsKey(baseName)) return this.homMap.get(baseName).stream();
        return Stream.empty();
    }

    /**
     * Retrieve the number of homonyms of the given person name in the MMDB.
     *
     * @param name The name.
     * @return The number of homonymous names in dblp, or {@code 0} if the name is not in dblp.
     */
    int numberOfHomonyms(String name) {
        String baseName = stripHomonymNumber(name);
        if (this.homMap.containsKey(baseName)) return this.homMap.get(baseName).size();
        return 0;
    }

    /**
     * Strips the homonym number from a dblp name string.
     *
     * @param name The name
     * @return The name without any homonym number.
     */
    private static String stripHomonymNumber(String name) {
        if (HOM_PATTERN.matcher(name).matches()) return name.substring(0, name.length() - 5);
        return name;
    }

}
