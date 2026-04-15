package com.csci201.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads variables from a {@code .env} file in the process working directory into the Spring
 * {@link org.springframework.core.env.Environment} (highest precedence). Skipped when
 * {@code -Dspring.dotenv.skip=true} (set by Surefire during {@code mvn test}) so local {@code .env}
 * files do not change test behavior.
 *
 * <p><strong>Order:</strong> runs {@link Ordered#LOWEST_PRECEDENCE} minus 1 so this runs
 * <em>after</em> {@code ConfigDataEnvironmentPostProcessor} (which loads {@code application.yml}).
 * Then {@link #addFirst} prepends our property source so explicit {@code spring.datasource.*} keys
 * override YAML. If this ran before config data, config data could win and you would see
 * {@code using password: NO} despite a correct {@code .env}.
 *
 * <p>Diagnostics use {@link BootstrapLog} (stderr) because SLF4J is often not initialized during
 * this phase.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    private static final String LOG_SECRETS_PROPERTY = "dotenv.log.secrets";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.getBoolean("spring.dotenv.skip")) {
            BootstrapLog.line("dotenv: skipped (spring.dotenv.skip=true)");
            return;
        }

        Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path envFile = cwd.resolve(".env");
        boolean envFileExists = Files.isRegularFile(envFile);

        Dotenv dotenv = Dotenv.configure().directory(cwd.toString()).ignoreIfMissing().load();
        Map<String, Object> map = new LinkedHashMap<>();
        dotenv.entries()
                .forEach(
                        entry ->
                                map.put(
                                        entry.getKey() == null ? "" : entry.getKey().trim(),
                                        entry.getValue() == null ? "" : entry.getValue()));

        BootstrapLog.line("dotenv: working directory = " + cwd);
        BootstrapLog.line("dotenv: .env path = " + envFile + " (exists=" + envFileExists + ")");

        if (map.isEmpty()) {
            BootstrapLog.line(
                    "dotenv: no entries loaded — create .env in project root or fix format. "
                            + "Spring will use YAML defaults (e.g. root / empty password).");
            return;
        }

        logAllKeys(map);
        syncSpringDatasourceProperties(map);

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
        BootstrapLog.line("dotenv: loaded " + map.size() + " variable(s) into property source '" + PROPERTY_SOURCE_NAME + "'");

        logDotenvDatabaseHints(map);
    }

    /**
     * After {@code ConfigDataEnvironmentPostProcessor} (order +10) so {@code spring.datasource.*}
     * we set here wins when {@link #addFirst} runs.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    private static void syncSpringDatasourceProperties(Map<String, Object> map) {
        if (isH2Profile(map)) {
            BootstrapLog.line(
                    "dotenv: spring.datasource.* sync skipped — h2 profile active (remove SPRING_PROFILES_ACTIVE=h2 from .env to use MySQL from .env)");
            return;
        }

        Object dbUser = getIgnoreCase(map, "DB_USER");
        Object dbPassword = resolveDbPassword(map);
        if (dbUser != null) {
            map.put("spring.datasource.username", String.valueOf(dbUser));
            BootstrapLog.line("dotenv: set spring.datasource.username from DB_USER");
        }

        boolean hasPasswordKey =
                containsKeyIgnoreCase(map, "DB_PASSWORD") || containsKeyIgnoreCase(map, "MYSQL_PASSWORD");
        if (hasPasswordKey) {
            map.put("spring.datasource.password", dbPassword == null ? "" : String.valueOf(dbPassword));
            String source =
                    containsKeyIgnoreCase(map, "DB_PASSWORD") ? "DB_PASSWORD" : "MYSQL_PASSWORD";
            BootstrapLog.line(
                    "dotenv: set spring.datasource.password from "
                            + source
                            + " (length="
                            + (dbPassword == null ? 0 : String.valueOf(dbPassword).length())
                            + ")");
        } else {
            BootstrapLog.line(
                    "dotenv: WARN no DB_PASSWORD or MYSQL_PASSWORD in .env — password not applied; add DB_PASSWORD=...");
        }

        String host = firstString(getIgnoreCase(map, "MYSQL_HOST"), "localhost");
        String port = firstString(getIgnoreCase(map, "MYSQL_PORT"), "3306");
        String database = firstString(getIgnoreCase(map, "MYSQL_DATABASE"), "csci201");
        String url =
                "jdbc:mysql://"
                        + host
                        + ":"
                        + port
                        + "/"
                        + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        map.put("spring.datasource.url", url);
        BootstrapLog.line(
                "dotenv: set spring.datasource.url from MYSQL_* (host=" + host + ", port=" + port + ", database=" + database + ")");
    }

    private static boolean isH2Profile(Map<String, Object> map) {
        Object p = getIgnoreCase(map, "SPRING_PROFILES_ACTIVE");
        if (p != null && profileListContains(p, "h2")) {
            BootstrapLog.line("dotenv: detected profile h2 from SPRING_PROFILES_ACTIVE in .env");
            return true;
        }
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        if (env != null && profileListContains(env, "h2")) {
            BootstrapLog.line("dotenv: detected profile h2 from env SPRING_PROFILES_ACTIVE");
            return true;
        }
        String prop = System.getProperty("spring.profiles.active");
        if (prop != null && profileListContains(prop, "h2")) {
            BootstrapLog.line("dotenv: detected profile h2 from -Dspring.profiles.active");
            return true;
        }
        return false;
    }

    private static boolean profileListContains(Object raw, String profile) {
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return false;
        }
        for (String part : s.split(",")) {
            if (profile.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static Object resolveDbPassword(Map<String, Object> map) {
        Object fromDb = getIgnoreCase(map, "DB_PASSWORD");
        if (fromDb != null) {
            return fromDb;
        }
        return getIgnoreCase(map, "MYSQL_PASSWORD");
    }

    private static void logAllKeys(Map<String, Object> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        BootstrapLog.line("dotenv: parsed keys (" + keys.size() + "): " + keys);
    }

    private static Object getIgnoreCase(Map<String, Object> map, String wanted) {
        if (map.containsKey(wanted)) {
            return map.get(wanted);
        }
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(wanted)) {
                BootstrapLog.line("dotenv: using key '" + e.getKey() + "' for '" + wanted + "' (case mismatch; prefer UPPER_SNAKE_CASE in .env)");
                return e.getValue();
            }
        }
        return null;
    }

    private static boolean containsKeyIgnoreCase(Map<String, Object> map, String wanted) {
        if (map.containsKey(wanted)) {
            return true;
        }
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }

    private static String firstString(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value).trim();
    }

    private static void logDotenvDatabaseHints(Map<String, Object> map) {
        Object dbUser = getIgnoreCase(map, "DB_USER");
        Object dbPassword = resolveDbPassword(map);
        Object mysqlHost = getIgnoreCase(map, "MYSQL_HOST");
        Object mysqlPort = getIgnoreCase(map, "MYSQL_PORT");
        Object mysqlDb = getIgnoreCase(map, "MYSQL_DATABASE");

        BootstrapLog.line("dotenv: DB_USER raw = " + (dbUser != null ? "\"" + dbUser + "\"" : "(not set in .env)"));
        BootstrapLog.line("dotenv: DB_PASSWORD raw = " + describeSecret(dbPassword));

        if (Boolean.getBoolean(LOG_SECRETS_PROPERTY)
                || Boolean.parseBoolean(System.getenv("DOTENV_LOG_SECRETS"))) {
            BootstrapLog.line("dotenv: WARN DOTENV_LOG_SECRETS — full DB_PASSWORD = [" + dbPassword + "]");
        }

        BootstrapLog.line(
                "dotenv: MYSQL_HOST="
                        + (mysqlHost != null ? mysqlHost : "(not set)")
                        + ", MYSQL_PORT="
                        + (mysqlPort != null ? mysqlPort : "(not set)")
                        + ", MYSQL_DATABASE="
                        + (mysqlDb != null ? mysqlDb : "(not set)"));
    }

    private static String describeSecret(Object value) {
        if (value == null) {
            return "(not set in .env — YAML default may apply)";
        }
        String s = String.valueOf(value);
        int len = s.length();
        if (len == 0) {
            return "(empty string in .env)";
        }
        return "length="
                + len
                + " (masked; set DOTENV_LOG_SECRETS=true or -D"
                + LOG_SECRETS_PROPERTY
                + "=true to log full value)";
    }
}
