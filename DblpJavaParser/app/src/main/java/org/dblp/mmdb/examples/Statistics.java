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
package org.dblp.mmdb.examples;

import java.io.IOException;

import org.dblp.mmdb.RecordDb;
import org.dblp.mmdb.RecordDbInterface;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
public class Statistics {

    public static void main(String[] args) throws IOException, SAXException, InterruptedException {
        System.setProperty("entityExpansionLimit", "2000000");

        System.err.print("starting in:");
        for (int i = 10; i > 0; i--) {
            if (i < 10) System.err.print(" -");
            System.err.print(" " + i);
            Thread.sleep(1000);
        }
        System.err.println(" - go!");

        if (args.length < 2) {
            System.out.format("Usage: java %s <dblp-xml> <dblp-dtd>\n", Statistics.class.getName());
            System.exit(0);
        }
        System.out.println("building statistic for " + args[0]);
        RecordDbInterface dblp = new RecordDb(args[0], args[1], true);

        System.out.println("#publications = " + dblp.numberOfPublications());
        System.out.println("#persons = " + dblp.numberOfPersons());
        System.out.println("#journals = " + dblp.getJournals().size());
        System.out.println("#booktitles = " + dblp.getBookTitles().size());

        System.out.println("#coauthors of Kurt Mehlhorn = "
                + dblp.numberOfCoauthors(dblp.getPersonByName("Kurt Mehlhorn")));

        System.out.println("#coauthors of Manfred Jackel = "
                + dblp.numberOfCoauthors(dblp.getPersonByName("Manfred Jackel")));
        System.gc();
    }

}
