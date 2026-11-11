package edu.nipa.scasp.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationKey;
import edu.nipa.scasp.domain.RegistrationStatus;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private static final AtomicInteger DATABASES = new AtomicInteger();
    private JdbcDataSource dataSource;
    private JdbcRegistrationService registrations;
    private JdbcStudentRepository students;
    private JdbcUserRepository users;
    private JdbcCourseRepository courses;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:scasp" + DATABASES.incrementAndGet()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        applySchema();
        registrations = new JdbcRegistrationService(dataSource);
        students = new JdbcStudentRepository(dataSource);
        users = new JdbcUserRepository(dataSource);
        courses = new JdbcCourseRepository(dataSource);
        addStudent("student-1", "user-1");
        addStudent("student-2", "user-2");
        courses.save("CS101", new Course("CS101", "Computing", 1, CourseStatus.OPEN));
    }

    @Test
    void registrationWaitlistPromotionAndAuditCommitTogether() throws Exception {
        User first = user("user-1");
        User second = user("user-2");
        RegistrationAttempt enrolled = registrations.register(first, "student-1", "CS101", NOW);
        RegistrationAttempt queued = registrations.register(second, "student-2", "CS101",
                NOW.plusSeconds(1));
        registrations.withdraw(first, "student-1", "CS101", NOW.plusSeconds(2));

        assertEquals(RegistrationStatus.ENROLLED, registrationStatus("student-2"));
        assertEquals(RegistrationStatus.WITHDRAWN, registrationStatus("student-1"));
        assertEquals(0, count("SELECT COUNT(*) FROM waitlist_entries"));
        assertEquals(3, count("SELECT COUNT(*) FROM audit_events"));
        assertEquals(RegistrationStatus.ENROLLED, enrolled.registration().status());
        assertTrue(queued.isWaitlisted());
        assertThrows(IllegalStateException.class, () -> registrations.register(second,
                "student-2", "CS101", NOW.plusSeconds(3)));
    }

    @Test
    void registrationRepositorySupportsCrud() {
        JdbcRegistrationRepository repository = new JdbcRegistrationRepository(dataSource);
        Registration registration = new Registration(java.util.UUID.randomUUID(), "student-1",
                "CS101", RegistrationStatus.ENROLLED, NOW);
        RegistrationKey key = new RegistrationKey("student-1", "CS101");

        repository.save(key, registration);
        assertEquals(registration, repository.findById(key).orElseThrow());
        assertEquals(1, repository.findAll().size());
        Registration withdrawn = registration.transitionTo(RegistrationStatus.WITHDRAWN);
        repository.save(key, withdrawn);
        assertEquals(RegistrationStatus.WITHDRAWN, repository.findById(key).orElseThrow().status());
        assertTrue(repository.deleteById(key));
        assertTrue(repository.findById(key).isEmpty());
    }

    @Test
    void auditFailureRollsBackRegistrationChanges() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DROP TABLE audit_events")) {
            statement.execute();
        }

        assertThrows(DataAccessException.class, () -> registrations.register(user("user-1"),
                "student-1", "CS101", NOW));
        assertEquals(0, count("SELECT COUNT(*) FROM registrations"));
    }

    @Test
    void preparedCourseSearchDoesNotInterpretInputAsSql() {
        assertTrue(courses.search("' OR 1=1 --").isEmpty());
        courses.save("CS202", new Course("CS202", "Data Structures", 25, CourseStatus.OPEN));
        assertEquals(1, courses.search("structure").size());
    }

    @Test
    void reportsJoinCourseStudentAndRequestData() throws Exception {
        registrations.register(user("user-1"), "student-1", "CS101", NOW);
        JdbcReports reports = new JdbcReports(dataSource);

        assertEquals(1, reports.courseCapacity().getFirst().enrolled());
        assertEquals("student-1", reports.studentRegistrations().getFirst().studentId());
        assertTrue(reports.serviceRequestWorkload().isEmpty());
    }

    @Test
    void databaseCapacityIsPreservedUnderConcurrentRegistrations() throws Exception {
        addStudent("student-3", "user-3");
        addStudent("student-4", "user-4");
        List<Callable<RegistrationAttempt>> attempts = List.of(
                () -> registrations.register(user("user-3"), "student-3", "CS101", NOW),
                () -> registrations.register(user("user-4"), "student-4", "CS101", NOW));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var results = executor.invokeAll(attempts, 8, TimeUnit.SECONDS);
            assertEquals(2, results.size());
            assertEquals(1, results.stream().filter(future -> {
                try {
                    return !future.get().isWaitlisted();
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            }).count());
            assertEquals(1, count("SELECT COUNT(*) FROM registrations WHERE status = 'ENROLLED'"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void jdbcAuthenticationPersistsOnlySaltedPasswordHashes() throws Exception {
        JdbcAuthenticationService authentication = new JdbcAuthenticationService(dataSource);
        User account = new User("auth-user", "Demo Login", UserRole.STUDENT, UserStatus.ACTIVE);
        authentication.register(account, "correct horse battery".toCharArray());

        assertEquals(account, authentication.authenticate("auth-user",
                "correct horse battery".toCharArray()));
        assertThrows(SecurityException.class, () -> authentication.authenticate("auth-user",
                "incorrect password".toCharArray()));
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password_salt, password_hash FROM users WHERE user_id = ?")) {
            statement.setString(1, "auth-user");
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertTrue(result.getBytes("password_salt").length >= 16);
                assertTrue(result.getBytes("password_hash").length >= 32);
            }
        }
    }

    @Test
    void jdbcCourseManagementEnforcesStaffAuthorizationAndAuditsChanges() throws Exception {
        User staff = new User("staff-1", "Academic Staff", UserRole.STAFF, UserStatus.ACTIVE);
        users.save(staff.userId(), staff);
        JdbcCampusBackend backend = new JdbcCampusBackend(dataSource);
        backend.createOrUpdateCourse(staff,
                new Course("CS202", "Data Structures", 20, CourseStatus.OPEN));
        assertEquals(20, courses.findById("CS202").orElseThrow().capacity());
        assertEquals(1, count("SELECT COUNT(*) FROM audit_events WHERE entity_type = 'Course'"));

        assertThrows(SecurityException.class, () -> backend.createOrUpdateCourse(user("user-1"),
                new Course("CS202", "Data Structures", 25, CourseStatus.OPEN)));
        assertThrows(java.sql.SQLException.class, () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO courses (course_code, title, capacity, status) "
                                 + "VALUES (?, ?, ?, ?)")) {
                statement.setString(1, "BAD");
                statement.setString(2, "Invalid Capacity");
                statement.setInt(3, 0);
                statement.setString(4, "OPEN");
                statement.executeUpdate();
            }
        });
    }

    private void addStudent(String studentId, String userId) {
        User user = new User(userId, userId, UserRole.STUDENT, UserStatus.ACTIVE);
        users.save(userId, user);
        students.save(studentId, new Student(studentId, userId, "Computing", 1));
    }

    private static User user(String userId) {
        return new User(userId, userId, UserRole.STUDENT, UserStatus.ACTIVE);
    }

    private RegistrationStatus registrationStatus(String studentId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT status FROM registrations WHERE student_id = ?")) {
            statement.setString(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return RegistrationStatus.valueOf(result.getString(1));
            }
        }
    }

    private int count(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private void applySchema() throws Exception {
        String schema = Files.readString(Path.of("..", "database", "schema.sql"));
        List<String> statements = new ArrayList<>();
        for (String statement : schema.split(";")) {
            if (!statement.isBlank()) {
                statements.add(statement);
            }
        }
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }
}
