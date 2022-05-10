package org.dblp.mmdb;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple logger stand-in for {@link Mmdb}, since {@link java.util.logging.Logger} is crap.
 *
 * @author mra
 */
class MmdbLogger {

    /** The current maximum number of log messages to be printed per category. */
    private final int maxMessages;
    /** The current log level. */
    private MmdbLogLevel level;
    /** The current output print stream. */
    private final PrintStream out;
    /** The log message counts. */
    private final Map<String, Integer> messageCounts;

    /**
     * Create a new logger of the given log level.
     *
     * @param lvl The log level.
     * @param out The output stream.
     * @param max The maximum number of messages to be printed per category.
     * @throws IllegalArgumentException if {@code max < 0}.
     */
    MmdbLogger(MmdbLogLevel lvl, PrintStream out, int max) throws IllegalArgumentException {
        if (max < 0)
            throw new IllegalArgumentException("maximum number of messages must be non-negative");

        this.level = lvl;
        this.out = out;
        this.messageCounts = new HashMap<>();
        this.maxMessages = max;
    }

    /**
     * Retrieves the current log level.
     *
     * @return The log level.
     */
    MmdbLogLevel getLevel() {
        return this.level;
    }

    /**
     * Sets the log level.
     *
     * @param lvl The log level.
     */
    void setLevel(MmdbLogLevel lvl) {
        this.level = lvl;
    }

    /**
     * Log the given message.
     *
     * @param lvl The log level.
     * @param category The category.
     * @param message The message.
     */
    private void log(MmdbLogLevel lvl, String category, Object message) {
        if (this.level.ordinal() < lvl.ordinal()) return;

        int count = 0;
        if (this.messageCounts.containsKey(category)) {
            count = this.messageCounts.get(category);
        }

        if (count < this.maxMessages) {
            this.out.format("[%s] %s: %s\n", lvl, category, message);
        }
        else if (count == this.maxMessages) {
            this.out.format("maximum of %dx '%s' reached, omitting further messages\n", this.maxMessages, category);
        }
        this.messageCounts.put(category, count + 1);
    }

    /**
     * Log the given message of level {@link MmdbLogLevel#TRACE}.
     *
     * @param category The category.
     * @param message The message.
     */
    void trace(String category, Object message) {
        log(MmdbLogLevel.TRACE, category, message);
    }

    /**
     * Log the given message of level {@link MmdbLogLevel#DEBUG}.
     *
     * @param category The category.
     * @param message The message.
     */
    void debug(String category, Object message) {
        log(MmdbLogLevel.DEBUG, category, message);
    }

    /**
     * Log the given message of level {@link MmdbLogLevel#INFO}.
     *
     * @param category The category.
     * @param message The message.
     */
    void info(String category, Object message) {
        log(MmdbLogLevel.INFO, category, message);
    }

    /**
     * Log the given message of level {@link MmdbLogLevel#WARN}.
     *
     * @param category The category.
     * @param message The message.
     */
    void warn(String category, Object message) {
        log(MmdbLogLevel.WARN, category, message);
    }

    /**
     * Log the given message of level {@link MmdbLogLevel#ERROR}.
     *
     * @param category The category.
     * @param message The message.
     */
    void error(String category, Object message) {
        log(MmdbLogLevel.ERROR, category, message);
    }

    /**
     * Log the given message of level {@link MmdbLogLevel#FATAL}.
     *
     * @param category The category.
     * @param message The message.
     */
    void fatal(String category, Object message) {
        log(MmdbLogLevel.FATAL, category, message);
    }

    /**
     * Log the current stack trace.
     */
    void printCurrentStackTrace() {
        this.out.println("StackTrace:");
        boolean first = true;
        for (StackTraceElement trace : Thread.currentThread().getStackTrace()) {
            if (first) {
                first = false;
                continue;
            }
            this.out.println("  " + trace.toString());
        }
    }

}
