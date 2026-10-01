package net.javaguides.springboot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${spring.datasource.driverClassName:}")
    private String configuredDriverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Priority 1: Check DATABASE_URL (Render, Railway, Heroku standard environment variable)
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = configuredUrl;
        }

        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = "jdbc:h2:file:./data/chatdb;AUTO_SERVER=TRUE";
        }

        HikariConfig config = new HikariConfig();

        // Check if it's a PostgreSQL URL
        if (isPostgreSqlUrl(envDbUrl)) {
            configurePostgreSql(config, envDbUrl);
        } else {
            // Local H2 or standard custom JDBC URL
            config.setJdbcUrl(envDbUrl);
            config.setUsername(configuredUsername != null && !configuredUsername.isBlank() ? configuredUsername : "sa");
            config.setPassword(configuredPassword != null ? configuredPassword : "");
            if (configuredDriverClassName != null && !configuredDriverClassName.isBlank()) {
                config.setDriverClassName(configuredDriverClassName);
            }
        }

        config.setPoolName("TalkFlowHikariPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        logger.info("Initializing DataSource with JDBC URL: {}", maskUrl(config.getJdbcUrl()));
        return new HikariDataSource(config);
    }

    private boolean isPostgreSqlUrl(String url) {
        String trimmed = url.trim().toLowerCase();
        return trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://") || trimmed.startsWith("jdbc:postgresql://");
    }

    private void configurePostgreSql(HikariConfig config, String rawUrl) {
        config.setDriverClassName("org.postgresql.Driver");

        String cleanedUrl = rawUrl.trim();
        if (cleanedUrl.startsWith("jdbc:")) {
            cleanedUrl = cleanedUrl.substring(5);
        }

        try {
            URI uri = new URI(cleanedUrl);

            String userInfo = uri.getUserInfo();
            String username = configuredUsername;
            String password = configuredPassword;

            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    username = parts[0];
                }
                if (parts.length > 1) {
                    password = parts[1];
                }
            }

            if ((username == null || username.isBlank()) && System.getenv("SPRING_DATASOURCE_USERNAME") != null) {
                username = System.getenv("SPRING_DATASOURCE_USERNAME");
            }
            if ((password == null || password.isBlank()) && System.getenv("SPRING_DATASOURCE_PASSWORD") != null) {
                password = System.getenv("SPRING_DATASOURCE_PASSWORD");
            }

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String query = uri.getQuery();

            StringBuilder jdbcUrl = new StringBuilder();
            jdbcUrl.append("jdbc:postgresql://").append(host).append(":").append(port).append(path);
            if (query != null && !query.isBlank()) {
                jdbcUrl.append("?").append(query);
            } else {
                jdbcUrl.append("?sslmode=require");
            }

            config.setJdbcUrl(jdbcUrl.toString());
            if (username != null && !username.isBlank()) {
                config.setUsername(username);
            }
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }
            logger.info("Configured PostgreSQL DataSource for host: {}, user: {}", host, username);
        } catch (Exception e) {
            logger.warn("Failed to parse URI ({}), attempting fallback prefixing: {}", e.getMessage(), rawUrl);
            String jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
            config.setJdbcUrl(jdbcUrl);
            if (configuredUsername != null && !configuredUsername.isBlank()) {
                config.setUsername(configuredUsername);
            }
            if (configuredPassword != null && !configuredPassword.isBlank()) {
                config.setPassword(configuredPassword);
            }
        }
    }

    private String maskUrl(String url) {
        if (url == null) return null;
        return url.replaceAll(":[^/@:]+@", ":****@");
    }
}
