package com.xanwar.rps.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * When Render links a Postgres database it sets DATABASE_URL (postgres://...).
 * Spring needs jdbc:postgresql://... — this converts it if SPRING_DATASOURCE_URL is not set.
 */
@Configuration
@ConditionalOnExpression("'${SPRING_DATASOURCE_URL:}'.isBlank() && !'${DATABASE_URL:}'.isBlank()")
public class RenderDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource renderDataSource(
            @org.springframework.beans.factory.annotation.Value("${DATABASE_URL}") String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL is set but empty");
        }
        URI uri = URI.create(normalizeScheme(databaseUrl));
        String username = decode(userInfoPart(uri, 0));
        String password = decode(userInfoPart(uri, 1));
        String jdbcUrl = toJdbcUrl(uri);

        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    private static String normalizeScheme(String url) {
        if (url.startsWith("postgres://")) {
            return "postgresql://" + url.substring("postgres://".length());
        }
        return url;
    }

    private static String userInfoPart(URI uri, int index) {
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return "";
        }
        String[] parts = userInfo.split(":", 2);
        return index < parts.length ? parts[index] : "";
    }

    private static String decode(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    static String toJdbcUrl(URI uri) {
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath() == null ? "" : uri.getPath();
        String query = uri.getQuery();
        String jdbcQuery = (query == null || query.isBlank())
                ? "sslmode=require"
                : (query.contains("sslmode") ? query : query + "&sslmode=require");
        return "jdbc:postgresql://" + host + ":" + port + path + "?" + jdbcQuery;
    }
}
