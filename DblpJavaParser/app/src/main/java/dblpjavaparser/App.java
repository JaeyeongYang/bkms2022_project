package dblpjavaparser;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.dblp.mmdb.*;
import org.neo4j.driver.*;

import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.*;
import net.sourceforge.argparse4j.inf.*;

@SuppressWarnings("javadoc")
class App implements AutoCloseable {
    protected static boolean flag_store_all;
    private final Driver driver;

    private final Collection<String> FIELDS_EXCLUDED_FOR_PUBL = List.of(
            "author", "editor", "ee", "isbn", "note",
            "url", "cite", "crossref");

    public App(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    public void createConstraints() {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run("""
                    CREATE CONSTRAINT UniquePublication IF NOT EXISTS
                    ON (p: Publication) ASSERT p.key IS UNIQUE;
                    """));

            session.writeTransaction(tx -> tx.run("""
                    CREATE CONSTRAINT UniqueAuthor IF NOT EXISTS
                    ON (a: Author) ASSERT a.name IS UNIQUE;
                    """));
        }
    }

    public void createIndexes() {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run("""
                    CREATE INDEX PublicationIndex IF NOT EXISTS
                    FOR (p:Publication) ON (p.key)
                    """));

            session.writeTransaction(tx -> tx.run("""
                    CREATE INDEX AuthorIndex IF NOT EXISTS
                    FOR (a:Author) ON (a.name)
                    """));
        }
    }

    private Result createPublResult(Transaction tx, Publication publ, String streamKey, Collection<String> citedKeys) {
        Map<String, Object> params;
        if (flag_store_all) {
            params = publ.fields()
                    .filter(f -> !FIELDS_EXCLUDED_FOR_PUBL.contains(f.tag()))
                    .collect(Collectors.toMap(Field::tag, Field::value, (p1, p2) -> p1));
            params.put("type", publ.getTag());
        } else {
            params = new HashMap<String, Object>();
        }

        StringBuilder query = new StringBuilder();
        query.append("MERGE (p: Publication {key: $key})\n");
        if (streamKey != "") {
            query.append("MERGE (s: Stream {key: $streamKey})\n");
            query.append("MERGE (p)-[:GROUPED_BY]->(s)\n");
        }

        if (flag_store_all) {
            query.append("SET ");
            params.keySet().stream().forEach(k -> {
                query.append("p." + k + " = ");
                if (k.equals("year")) {
                    query.append("toInteger(");
                }
                query.append("$" + k);
                if (k.equals("year")) {
                    query.append(")");
                }
                query.append(", ");
            });
            query.setLength(query.length() - 2);
            query.append("\n");
        }
        params.put("key", publ.getKey());
        if (streamKey != "") {
            params.put("streamKey", streamKey);
        }

        if (citedKeys.size() > 0) {
            query.append("FOREACH (key_cited IN $citedKeys |\n");
            query.append("  MERGE (p_cited: Publication {key: key_cited})\n");
            query.append("  CREATE (p_cited) -[:CITED_BY]-> (p))\n");

            params.put("citedKeys", citedKeys);
        }

        return tx.run(query.toString(), params);
    };

    private Result createAuthorResult(Transaction tx, String publKey, Field authorField,
            int authorshipOrder, int numAuthors) {
        Map<String, Object> params = authorField.attributes()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (p1, p2) -> p1));

        StringBuilder query = new StringBuilder();
        query.append("MATCH (p: Publication {key: $publKey})\n");

        query.append("MERGE (a: Author {name: $name}) ");
        if (flag_store_all && (params.size() > 0)) {
            query.append("SET ");
            params.keySet().stream().forEach(k -> {
                query.append("a." + k + " = $" + k + ", ");
            });
            query.setLength(query.length() - 2);
        }
        query.append("\n");

        query.append("CREATE (p) -[:AUTHORED_BY {order: $authorshipOrder, num_authors: $numAuthors}]-> (a)");

        params.put("name", authorField.value());
        params.put("publKey", publKey);
        params.put("authorshipOrder", authorshipOrder);
        params.put("numAuthors", numAuthors);

        return tx.run(query.toString(), params);
    }

    public void addPublicationToNeo4j(Publication publ) {
        String publKey = publ.getKey();
        String[] publKeyStrings = publKey.split("/");
        String streamKey = (publKeyStrings.length > 2)
                ? String.join("/", Arrays.copyOf(publKeyStrings, publKeyStrings.length - 1))
                : "";
        Collection<Field> authorFields = publ.getFields("author");
        Collection<String> citedKeys = publ.fields("cite")
                .filter(f -> !f.value().equals("..."))
                .map(f -> f.value()).toList();
        int numAuthors = authorFields.size();

        try (Session session = driver.session()) {
            session.writeTransaction(tx -> createPublResult(tx, publ, streamKey, citedKeys));

            AtomicInteger index = new AtomicInteger();
            authorFields.stream().forEach(authorField -> {
                int i = index.incrementAndGet();
                session.writeTransaction(tx -> createAuthorResult(tx, publKey, authorField,
                        i, numAuthors));
            });
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("entityExpansionLimit", "10000000");

        ArgumentParser parser = ArgumentParsers.newFor("DblpJavaParser").build()
                .defaultHelp(true)
                .description("Parse the DBLP XML file and upload to Neo4j");

        parser.addArgument("--host")
                .setDefault("bolt://ccsl1.snu.ac.kr:54010")
                .help("Host URI for the Neo4J database to upload.");
        parser.addArgument("--user")
                .setDefault("neo4j")
                .help("Username of the Neo4J database");
        parser.addArgument("--password")
                .setDefault("bkmsneo4j")
                .help("Password of the Neo4J database");
        parser.addArgument("--store-all")
                .dest("store_all")
                .action(Arguments.storeTrue())
                .help("Whether to store all properties into Neo4j");
        parser.addArgument("xmlFilename")
                .help("XML data file to parse");
        parser.addArgument("dtdFilename")
                .help("DTD schema file for the XML data");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String hosturi = ns.get("host");
        String username = ns.get("user");
        String password = ns.get("password");

        String dblpXmlFilename = ns.get("xmlFilename");
        String dblpDtdFilename = ns.get("dtdFilename");
        flag_store_all = ns.get("store_all");

        // Parse the XML file using org.dblp.mmdb.Mmdb without messages
        final PrintStream originalErr = System.err;
        PrintStream filterStream = new PrintStream(new OutputStream() {
            public void write(int b) {
                // NO-OP
            }
        });
        System.setErr(filterStream);
        Mmdb dblp = new Mmdb(dblpXmlFilename, dblpDtdFilename, false);
        System.setErr(originalErr);

        // System.out.format("DTD schema: %s\n", dblpDtdFilename);

        long startTime = System.currentTimeMillis();
        try (App app = new App(hosturi, username, password)) {
            // app.createConstraints();
            app.createIndexes();
            dblp.publications().forEach(p -> app.addPublicationToNeo4j(p));
        }
        long endTime = System.currentTimeMillis();

        System.out.format("%s\t%d\t", dblpXmlFilename, dblp.numberOfPublications());
        System.out.format("%.4f\n", (endTime - startTime) / 1000.0);
    }
}
