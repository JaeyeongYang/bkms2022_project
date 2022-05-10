package dblpjavaparser;

// Copyright (c)2015, dblp Team (University of Trier and
// Schloss Dagstuhl - Leibniz-Zentrum fuer Informatik GmbH)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// (1) Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// (2) Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// (3) Neither the name of the dblp team nor the names of its contributors
// may be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DBLP TEAM BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.dblp.mmdb.Record;
import org.dblp.mmdb.Field;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.RecordDb;
import org.dblp.mmdb.RecordDbInterface;
import org.dblp.mmdb.TableOfContents;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
class App {
    public static void main(String[] args) {
        // we need to raise entityExpansionLimit because the dblp.xml has millions of
        // entities
        System.setProperty("entityExpansionLimit", "10000000");

        if (args.length != 2) {
            System.err.format("Usage: java %s <dblp-xml-file> <dblp-dtd-file>\n", App.class.getName());
            System.exit(0);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        System.out.println("building the dblp main memory DB ...");
        RecordDbInterface dblp;
        long startTime = System.currentTimeMillis();
        try {
            dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, false);
        } catch (final IOException ex) {
            System.err.println("cannot read dblp XML: " + ex.getMessage());
            return;
        } catch (final SAXException ex) {
            System.err.println("cannot parse XML: " + ex.getMessage());
            return;
        }
        long endTime = System.currentTimeMillis();
        System.out.format("MMDB ready: %d publs, %d pers\n", dblp.numberOfPublications(), dblp.numberOfPersons());
        System.out.format("Time elapsed: %.2f (sec)\n\n", (endTime - startTime) / 1000.0);

        // system.out.println("finding most prolific author in dblp ...");
        // string prolificauthorname = null;
        // int prolificauthorcount = 0;
        // for (person pers : dblp.getpersons()) {
        // int publscount = pers.numberofpublications();
        // if (publscount > prolificauthorcount) {
        // prolificauthorcount = publscount;
        // prolificauthorname = pers.getprimaryname().name();
        // }
        // }
        // system.out.format("%s, %d records\n\n", prolificauthorname,
        // prolificauthorcount);

        // system.out.println("finding author with most coauthors in dblp ...");
        // string connectedauthorname = null;
        // int connectedauthorcount = 0;
        // for (person pers : dblp.getpersons()) {
        // int coauthorcount = dblp.numberofcoauthors(pers);
        // if (coauthorcount > connectedauthorcount) {
        // connectedauthorcount = coauthorcount;
        // connectedauthorname = pers.getprimaryname().name();
        // }
        // }
        // system.out.format("%s, %d coauthors\n\n", connectedauthorname,
        // connectedauthorcount);

        // system.out.println("finding coauthors of jim gray 0001 ...");
        // person jim = dblp.getpersonbyname("jim gray 0001");
        // for (int i = 0; i < dblp.numberofcoauthorcommunities(jim); i++) {
        // collection<person> coauthors = dblp.getcoauthorcommunity(jim, i);
        // system.out.format("group %d:\n", i);
        // for (person coauthor : coauthors) {
        // system.out.format(" %s\n", coauthor.getprimaryname().name());
        // }
        // }
        // system.out.println();

        // system.out.println("finding authors of focs 2010 ...");
        // comparator<person> cmp = (person o1,
        // person o2) ->
        // o1.getprimaryname().name().compareto(o2.getprimaryname().name());
        // map<person, integer> authors = new treemap<>(cmp);
        // tableofcontents focs2010toc = dblp.gettoc("db/conf/focs/focs2010.bht");
        // for (publication publ : focs2010toc.getpublications()) {
        // for (personname name : publ.getnames()) {
        // person pers = name.getperson();
        // if (authors.containskey(pers))
        // authors.put(pers, authors.get(pers) + 1);
        // else
        // authors.put(pers, 1);
        // }
        // }
        // for (person author : authors.keyset())
        // system.out.format(" %dx %s\n", authors.get(author),
        // author.getprimaryname().name());
        // system.out.println();

        // system.out.println("finding urls of focs 2010 publications ...");
        // for (publication publ : focs2010toc.getpublications()) {
        // for (field fld : publ.getfields("ee")) {
        // system.out.format(" %s\n", fld.value());
        // }
        // }

        int i = 0;
        for (Publication publ : dblp.getPublications()) {
            if (++i > 10) {
                break;
            }

            System.out.format("%s\n\n", publ.getXml());
        }

        System.out.println("done.");
    }
}
