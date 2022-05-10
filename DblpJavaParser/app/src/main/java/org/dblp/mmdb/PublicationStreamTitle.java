package org.dblp.mmdb;

/**
 * An abstract base class for dblp publication stream venues.
 */
public class PublicationStreamTitle {

    /** The long title of the publication stream. */
    protected String title;

    /**
     * Create a new typeless publication stream.
     *
     * @param title The title.
     */
    public PublicationStreamTitle(String title) {
        this.title = title;
    }

    /**
     * Retrieve the long title of the publication stream venue.
     *
     * @return The title.
     */
    public String getTitle() {
        return this.title;
    }
}
