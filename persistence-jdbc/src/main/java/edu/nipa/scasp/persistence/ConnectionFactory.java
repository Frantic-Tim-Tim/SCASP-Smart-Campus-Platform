package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

public final class ConnectionFactory {

    private final DatabaseConfig config;
    private final DataSource dataSource;

    // Used for standalone JDBC connections and database tests.
    public ConnectionFactory(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.dataSource = null;
    }

    // Used by WildFly with its managed datasource.
    public ConnectionFactory(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.config = null;
    }

    public Connection getConnection() throws SQLException {

        // Production: obtain a connection from WildFly's connection pool.
        if (dataSource != null) {
            return dataSource.getConnection();
        }

        // Standalone testing: use the supplied database configuration.
        return DriverManager.getConnection(
                config.url(),
                config.username(),
                config.password()
        );
    }
}