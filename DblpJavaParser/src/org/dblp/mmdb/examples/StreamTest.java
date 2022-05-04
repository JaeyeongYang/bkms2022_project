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
import java.util.Map;

import org.dblp.mmdb.Mmdb;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.Redirect;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
public class StreamTest {

    static void printAttributes(Map<String, String> ats) {
        if (ats == null) return;
        ats.forEach((k, v) -> System.out.println(" -- " + k + " = " + v));
    }

    static void printPublications(Mmdb dblp, String key) {
        Publication p = dblp.getPublication(key);
        if (p == null) {
            System.out.println("can not find publication record " + key);
            return;
        }
        System.out.println(p.getXml());
        System.out.println("   " + p.getTag());
        printAttributes(p.getAttributes());

        p.fields().forEach(f -> {
            System.out.println(f.tag() + ": " + f.value());
            printAttributes(f.getAttributes());
        });

        // int n = fr.getNumberOfFields();
        // for (int i = 0; i < n; i++) {
        // System.out.println(i + ": " + fr.getTag(i) + (fr.hasAttribute(i) ? "!" : " ")
        // + fr.getValue(i));
        // pr_attributes(fr.getAttributes(i));
        // }

    }

    public static void main(String[] args) throws IOException, SAXException {
        System.setProperty("entityExpansionLimit", "2000000");

        if (args.length < 1) {
            System.out.format("Usage: java %s <dblp-xml>\n", StreamTest.class.getName());
            System.exit(0);
        }
        Mmdb dblp = new Mmdb(args[0], true);

        printPublications(dblp, "journals/algorithmica/AckermannBKS14");
        printPublications(dblp, "conf/rocling/LiuCWHC13");
        printPublications(dblp, "conf/eusflat/Baczynski13");
        printPublications(dblp, "journals/dr/Jagadish99");
        printPublications(dblp, "journals/dam/StepienSSZ14");
        printPublications(dblp, "journals/jcheminf/Kochmann14");
        printPublications(dblp, "journals/dm/HamadaH95");
        printPublications(dblp, "conf/stacs/2011");
        System.out.println("\nRedirects ...");
        for (Redirect r: dblp.getRedirects()) {
            System.out.println(r.getKey() + " --> " + r.getFieldReader().valueOf("crossref"));
        }
        System.out.println();
        printPublications(dblp, "homepages/00/7");
        Redirect r = dblp.getRedirect("homepages/00/7");
        if (r == null)
            System.out.println("cann't find redirect homepages/00/7");
        else {
            System.out.println("found redirect record homepages/00/7");
            // Person p = dblp.getPerson(r.getFieldReader().valueOf("crossref"));
            Person p = r.getTarget();
            if (p == null)
                System.out.println("empty target");
            else {
                PersonName n = p.getPrimaryName();
                System.out.println("homepages/00/7 ===> " + n.name());
            }
        }
            
    }

}
