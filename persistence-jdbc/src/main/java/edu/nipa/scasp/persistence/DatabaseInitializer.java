package edu.nipa.scasp.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class DatabaseInitializer {

    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory"
        );
    }

    public void initialize() throws SQLException {
        String schema = loadSchema();

        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {

            for (String sql : schema.split(";")) {
                String command = sql.trim();

                if (!command.isEmpty()) {
                    statement.execute(command);
                }
            }
        }
    }

    private String loadSchema() {
        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("db/schema.sql")) {

            if (input == null) {
                throw new IllegalStateException(
                        "Database schema resource db/schema.sql was not found"
                );
            }

            return new String(input.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read database schema", exception
            );
        }
    }
}