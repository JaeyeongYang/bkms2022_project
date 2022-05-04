package org.dblp.mmdb.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dblp.mmdb.Mmdb;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.TableOfContents;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
public class Tocs {

    public static void main(String[] args) throws IOException, SAXException {
        System.setProperty("entityExpansionLimit", "2000000");

        if (args.length < 1) {
            System.out.format("Usage: java %s <dblp-xml>\n", Cites.class.getName());
            System.exit(0);
        }
        Mmdb db = new Mmdb(args[0], true);

        System.out.println("# tocs: " + db.numberOfTocs());
        System.out.println();

        List<TableOfContents> emptyTocs = new ArrayList<>();
        for (TableOfContents toc : db.getTocs()) {
            if (toc.size() == 0) emptyTocs.add(toc);
        }
        System.out.format("empty tocs: %d\n", emptyTocs.size());
        for (TableOfContents toc : emptyTocs) {
            System.out.println("* " + toc.getKey());
        }
        System.out.println();

        String maxTocKey = db.tocs().max((TableOfContents toc1,
                TableOfContents toc2) -> toc1.size()
                - toc2.size()).get().getKey();
        System.out.println("max toc key: " + maxTocKey);
        int maxTocSize = db.tocs().mapToInt(TableOfContents::size).max().getAsInt();
        System.out.println("max toc size: " + maxTocSize);
        System.out.println();

        String recKey = "journals/pvldb/Ley09";
        System.out.format("toc of %s: %s\n", recKey, db.getPublication(recKey).getToc().getKey());
        System.out.println();

        String tocKey = "db/conf/focs/focs2000";
        System.out.format("content of %s: %d\n", tocKey, db.getToc(tocKey).size());
        for (Publication publ : db.getToc(tocKey).getPublications()) {
            System.out.println("* " + publ.getKey());
        }
    }
}
