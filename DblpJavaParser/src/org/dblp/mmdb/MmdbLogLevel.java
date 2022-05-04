package org.dblp.mmdb;

/**
 * The log levels for {@link MmdbLogger}.
 *
 * @author mra
 */
enum MmdbLogLevel {
    /** The highest possible rank, intended to turn off logging. */
    OFF,
    /** Designates very severe error events that will lead to abort the application. */
    FATAL,
    /** Designates error events that might still allow the application to continue. */
    ERROR,
    /** Designates potentially harmful situations. */
    WARN,
    /** Designates informational messages that highlight progress. */
    INFO,
    /** Designates fine-grained informational events that are useful to debug. */
    DEBUG,
    /** Designates even finer-grained informational events than {@link #DEBUG}. */
    TRACE,
    /** The lowest possible rank, intended to turn on all logging. */
    ALL
};
