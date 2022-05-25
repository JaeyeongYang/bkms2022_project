package dblpjavaparser;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.dblp.mmdb.*;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
class DataSplitter {

    public static void main(String[] args) throws IOException, SAXException {
        // we need to raise entityExpansionLimit because the dblp.xml has millions of
        // entities
        System.setProperty("entityExpansionLimit", "10000000");

        if (args.length != 3) {
            System.err.format("Usage: java %s <dblp-xml-file> <dblp-dtd-file>\n", App.class.getName());
            System.exit(0);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];
        Path outputPath = Paths.get(args[2]);

        System.out.println("building the dblp main memory DB ...");

        long startTime = System.currentTimeMillis();
        Mmdb dblp = new Mmdb(dblpXmlFilename, dblpDtdFilename, true);
        long endTime = System.currentTimeMillis();

        System.out.format("MMDB ready: %d publs, %d pers\n", dblp.numberOfPublications(), dblp.numberOfPersons());
        System.out.format("Time elapsed: %.2f (sec)\n\n", (endTime - startTime) / 1000.0);

        Comparator<Publication> cmp = Comparator.comparing(Publication::getMdate, String.CASE_INSENSITIVE_ORDER);
        dblp.publications().sorted(cmp).forEach(p -> {
            String mdate = p.getMdate();

            File dirYear = outputPath.resolve(mdate.substring(0, 4)).toFile();
            if (!dirYear.exists()) {
                dirYear.mkdirs();
            }

            File fileOut = Paths.get(dirYear.getPath(), String.format("data_%s.txt",
                    mdate)).toFile();
            try (FileOutputStream outputStream = new FileOutputStream(fileOut, true)) {
                outputStream.write((p.getXml() + '\n').getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("done.");
    }
}
