package com.csci201.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Runs last among our EPPs (after {@link DotenvEnvironmentPostProcessor}) and logs merged
 * {@code spring.datasource.*} using stderr — SLF4J may not be ready during EPP.
 */
public class ResolvedDatasourceEnvironmentLoggerPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.getBoolean("spring.dotenv.skip")) {
            return;
        }

        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        BootstrapLog.line("resolved: spring.datasource.url = " + url);
        BootstrapLog.line("resolved: spring.datasource.username = " + username);
        BootstrapLog.line("resolved: spring.datasource.password = " + describePassword(password));

        if (Boolean.getBoolean("dotenv.log.secrets")
                || Boolean.parseBoolean(System.getenv("DOTENV_LOG_SECRETS"))) {
            BootstrapLog.line("resolved: WARN DOTENV_LOG_SECRETS — full password = [" + password + "]");
        }
    }

    private static String describePassword(String value) {
        if (value == null) {
            return "(null)";
        }
        if (value.isEmpty()) {
            return "(empty — MySQL will use password: NO; set DB_PASSWORD in .env)";
        }
        return "length=" + value.length() + " (masked; DOTENV_LOG_SECRETS=true for full value)";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
