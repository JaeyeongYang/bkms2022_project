package org.dblp.mmdb;

/**
 * A class representing dblp person redirection records.
 */
public class Redirect extends Record {

    /**
     * Create a new Redirect.
     *
     * @param dblp The MMDB.
     * @param key The record key.
     * @param mdate The record mdate.
     * @param xml The dblp record as byte array.
     */
    protected Redirect(MmdbInterface dblp, String key, int mdate, byte[] xml) {
        this.dblp = dblp;
        this.key = key;
        this.mdate = mdate;
        this.xml = xml;
    }

    /**
     * Retrieve the person target of this redirect.
     *
     * @return The person target
     * @throws IllegalStateException if the redirect cannot be resolved.
     */
    public Person getTarget() throws IllegalStateException {
        String key = this.getFieldReader().valueOf("crossref");
        if (key == null) throw new IllegalStateException("missing crossref field");
        Person target = this.dblp.getPerson(key);
        int count = 0;
        while (target == null) {
            if (count++ > 1000) throw new IllegalStateException("infinite reference loop");
            Redirect other = this.dblp.getRedirect(key);
            if (other == null) throw new IllegalStateException("no crossref target");
            key = other.getFieldReader().valueOf("crossref");
            if (key == null) throw new IllegalStateException("missing crossref field");
            target = this.dblp.getPerson(key);
        }
        return target;
    }

    @Override
    public String getXml() {
        return xmlBuild().toString();
    }
}
