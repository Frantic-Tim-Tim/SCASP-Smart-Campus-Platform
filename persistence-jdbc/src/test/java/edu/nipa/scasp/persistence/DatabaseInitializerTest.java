package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class DatabaseInitializerTest {

    @Test
    void initializeCreatesAllDomainTables() throws Exception {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:h2:mem:scasp_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        ConnectionFactory factory = new ConnectionFactory(config);
        DatabaseInitializer initializer = new DatabaseInitializer(factory);

        initializer.initialize();

        try (Connection connection = factory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' "
                     + "AND TABLE_NAME IN ("
                     + "'USERS', 'STUDENTS', 'COURSES', 'REGISTRATIONS', "
                     + "'WAITLIST_ENTRIES', 'SERVICE_REQUESTS', "
                     + "'ANNOUNCEMENTS', 'AUDIT_EVENTS', 'CAMPUS_SERVICES')"
             )) {

            result.next();
            assertEquals(9, result.getInt(1));
        }
    }
}