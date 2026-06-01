package com.xanwar.rps.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Fixes common Render mistakes: Postgres JDBC URL with MySQL driver, or user:pass inside the URL.
 */
public class JdbcUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROP_URL = "spring.datasource.url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty(PROP_URL));
        if (url == null || url.isBlank()) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();

        if (url.startsWith("jdbc:postgresql:")) {
            overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            overrides.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            ParsedJdbc parsed = parsePostgresJdbcUrl(url);
            if (parsed != null) {
                overrides.put(PROP_URL, parsed.jdbcUrl());
                if (!parsed.username().isBlank()) {
                    overrides.put("spring.datasource.username", parsed.username());
                }
                if (!parsed.password().isBlank()) {
                    overrides.put("spring.datasource.password", parsed.password());
                }
            }
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("jdbcUrlFix", overrides));
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * jdbc:postgresql://user:pass@host:5432/db -> separate fields + clean URL
     */
    static ParsedJdbc parsePostgresJdbcUrl(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            return null;
        }
        String remainder = jdbcUrl.substring("jdbc:postgresql://".length());
        int at = remainder.indexOf('@');
        if (at <= 0) {
            return null;
        }
        String userInfo = remainder.substring(0, at);
        String hostPart = remainder.substring(at + 1);
        int slash = hostPart.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String hostPort = hostPart.substring(0, slash);
        String pathAndQuery = hostPart.substring(slash);

        String[] creds = userInfo.split(":", 2);
        String username = decode(creds[0]);
        String password = creds.length > 1 ? decode(creds[1]) : "";

        String host = hostPort;
        int port = 5432;
        int colon = hostPort.indexOf(':');
        if (colon > 0) {
            host = hostPort.substring(0, colon);
            port = Integer.parseInt(hostPort.substring(colon + 1));
        }

        String query = pathAndQuery.contains("?") ? pathAndQuery.substring(pathAndQuery.indexOf('?') + 1) : "";
        String jdbcQuery = (query.isBlank() || !query.contains("sslmode"))
                ? (query.isBlank() ? "sslmode=require" : query + "&sslmode=require")
                : query;

        String path = pathAndQuery.contains("?") ? pathAndQuery.substring(0, pathAndQuery.indexOf('?')) : pathAndQuery;
        String cleanUrl = "jdbc:postgresql://" + host + ":" + port + path + "?" + jdbcQuery;

        return new ParsedJdbc(cleanUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record ParsedJdbc(String jdbcUrl, String username, String password) {}

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
