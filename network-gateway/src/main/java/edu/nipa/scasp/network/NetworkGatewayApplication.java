package edu.nipa.scasp.network;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.InMemoryCampusBackend;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.net.InetAddress;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;

public final class NetworkGatewayApplication {
    private NetworkGatewayApplication() {
    }

    public static void main(String[] args) throws Exception {
        InMemoryCampusBackend backend = new InMemoryCampusBackend();
        User student = new User("demo-student", "Fictional Demo Student", UserRole.STUDENT,
                UserStatus.ACTIVE);
        User staff = new User("demo-staff", "Fictional Demo Staff", UserRole.STAFF,
                UserStatus.ACTIVE);
        addUser(backend, student, requiredEnvironment("SCASP_DEMO_STUDENT_PASSWORD"));
        addUser(backend, staff, requiredEnvironment("SCASP_DEMO_STAFF_PASSWORD"));
        backend.addStudent(new Student("DEMO-001", student.userId(),
                "Information Systems", 1));
        backend.addCourse(staff, new Course("BACS3100", "Advanced Java Programming",
                40, CourseStatus.OPEN));
        int port = Integer.parseInt(System.getenv().getOrDefault("SCASP_PORT", "5050"));
        TcpCampusServer server = new TcpCampusServer(InetAddress.getLoopbackAddress(), port,
                15_000, backend, Clock.systemUTC());
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "scasp-server-shutdown"));
        System.out.println("SCASP TCP server listening on loopback port " + server.port());
        new CountDownLatch(1).await();
    }

    private static char[] requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.length() < 12) {
            throw new IllegalStateException(name + " must be set to at least 12 characters");
        }
        return value.toCharArray();
    }

    private static void addUser(InMemoryCampusBackend backend, User user, char[] password) {
        try {
            backend.addUser(user, password);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
