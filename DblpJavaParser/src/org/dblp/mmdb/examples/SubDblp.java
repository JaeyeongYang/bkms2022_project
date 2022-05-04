package org.dblp.mmdb.examples;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.RecordDb;
import org.dblp.mmdb.RecordDbInterface;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
public class SubDblp {

    public static void main(String[] args) {
        System.setProperty("entityExpansionLimit", "2000000");

        if (args.length != 2) {
            System.err.format("Usage: java %s <dblp-xml-file> <key-regex>\n", SubDblp.class.getName());
            System.exit(0);
        }
        String dblpXmlFile = args[0];
        String regex = args[1];

        System.err.println("building MMDB ...");
        RecordDbInterface dblp;
        try {
            dblp = new RecordDb(dblpXmlFile, false);
        }
        catch (final IOException ex) {
            System.err.println("cannot read dblp XML: " + ex.getMessage());
            return;
        }
        catch (final SAXException ex) {
            System.err.println("cannot parse XML: " + ex.getMessage());
            return;
        }
        System.err.format("MMDB ready: %d publs, %d pers\n", dblp.numberOfPublications(), dblp.numberOfPersons());

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String mdate = format.format(new Date());

        System.err.format("searching matches for regex: %s\n", regex);
        System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        System.out.println("<!DOCTYPE dblp SYSTEM \"/dblp/dblp.dtd\">");
        System.out.println("<dblp mdate=\"" + mdate + "\">");
        Set<String> keys = new HashSet<>();
        int count = 0;
        for (Publication publ : dblp.getPublications()) {
            String key = publ.getKey();
            if (!key.matches(regex)) continue;
            System.out.println(publ.getXml());
            for (PersonName name : publ.getNames()) {
                keys.add(name.getPerson().getKey());
                count++;
                if (count % 1000 == 0) System.err.format("found %s records ...\n", count);
            }
        }
        Pattern beginPattern = Pattern.compile("<person ");
        Pattern endPattern = Pattern.compile("</person>");
        for (String key : keys) {
            String rec = dblp.getRecord(key).getXml();
            rec = beginPattern.matcher(rec).replaceAll("<www ");
            rec = endPattern.matcher(rec).replaceAll("<title>Home Page</title></www>");
            System.out.println(rec);
        }
        System.out.println("</dblp>");
        System.err.format("total records written: %s\n", count);

        System.err.println("done.");
    }
}
