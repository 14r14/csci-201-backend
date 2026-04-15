package com.csci201.backend.config;

/**
 * Logs to stderr during {@link org.springframework.boot.env.EnvironmentPostProcessor} — SLF4J is
 * often not bound yet, so {@code Logger.info} can produce no output.
 */
final class BootstrapLog {

    private static final String PREFIX = "[csci201-backend] ";

    private BootstrapLog() {}

    static void line(String message) {
        System.err.println(PREFIX + message);
    }
}
