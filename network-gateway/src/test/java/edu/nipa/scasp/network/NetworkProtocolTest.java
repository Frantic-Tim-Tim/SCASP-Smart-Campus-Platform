package edu.nipa.scasp.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nipa.scasp.domain.InMemoryCampusBackend;
import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class NetworkProtocolTest {
    @Test
    void malformedCommandReturnsAValidationError() {
        String response = NetworkProtocol.handle("REGISTER|only-one-argument", null,
                new InMemoryCampusBackend(), Clock.systemUTC());

        assertTrue(response.startsWith("ERR|INVALID|"));
    }

    @Test
    void protectedCommandRequiresAuthentication() {
        String response = NetworkProtocol.handle("GET_STUDENT|student-1", null,
                new InMemoryCampusBackend(), Clock.systemUTC());

        assertTrue(response.startsWith("ERR|FORBIDDEN|"));
    }

    @Test
    void resourceClientRejectsUnapprovedHostsBeforeConnecting() {
        AuthorizedResourceClient client = new AuthorizedResourceClient(
                java.util.Set.of("example.org"), java.time.Duration.ofSeconds(1));

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
                () -> client.get(java.net.URI.create("https://not-approved.invalid/data")));
    }

    @Test
    void requiredStudentCommandsUseTheAuthenticatedBackend() {
        InMemoryCampusBackend backend = new InMemoryCampusBackend();
        User student = new User("user-1", "Demo Student", UserRole.STUDENT, UserStatus.ACTIVE);
        User staff = new User("staff-1", "Demo Staff", UserRole.STAFF, UserStatus.ACTIVE);
        backend.addUser(student, "correct horse battery".toCharArray());
        backend.addUser(staff);
        backend.addStudent(new Student("student-1", student.userId(), "Computing", 1));
        backend.addCourse(staff, new Course("CS101", "Programming", 1, CourseStatus.OPEN));
        User authenticated = backend.authenticate("user-1", "correct horse battery".toCharArray());

        assertTrue(NetworkProtocol.handle("GET_COURSES", authenticated, backend,
                Clock.systemUTC()).startsWith("OK|COURSES|1"));
        assertTrue(NetworkProtocol.handle("GET_STUDENT|student-1", authenticated, backend,
                Clock.systemUTC()).startsWith("OK|STUDENT|"));
        assertTrue(NetworkProtocol.handle("REGISTER|student-1|CS101", authenticated, backend,
                Clock.systemUTC()).startsWith("OK|ENROLLED|"));
        assertTrue(NetworkProtocol.handle("WITHDRAW|student-1|CS101", authenticated, backend,
                Clock.systemUTC()).startsWith("OK|WITHDRAWN|"));
    }
}
