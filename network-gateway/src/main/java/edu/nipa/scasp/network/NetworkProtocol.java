package edu.nipa.scasp.network;

import edu.nipa.scasp.domain.CampusBackend;
import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class NetworkProtocol {
    public static final int MAX_LINE_BYTES = 4096;
    private static final Logger LOGGER = Logger.getLogger(NetworkProtocol.class.getName());

    private NetworkProtocol() {
    }

    public static String handle(String line, User actor, CampusBackend backend, Clock clock) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(clock, "clock");
        if (line == null || line.isBlank() || line.length() > MAX_LINE_BYTES) {
            return error("INVALID", "Message is empty or too long");
        }
        String[] parts = line.split("\\|", -1);
        try {
            return switch (parts[0]) {
                case "GET_COURSES" -> courses(parts, backend);
                case "GET_STUDENT" -> student(parts, actor, backend);
                case "REGISTER" -> register(parts, actor, backend, clock);
                case "WITHDRAW" -> withdraw(parts, actor, backend, clock);
                default -> error("INVALID", "Unknown command");
            };
        } catch (SecurityException forbidden) {
            return error("FORBIDDEN", forbidden.getMessage());
        } catch (IllegalArgumentException invalid) {
            return error("INVALID", invalid.getMessage());
        } catch (IllegalStateException conflict) {
            return error("CONFLICT", conflict.getMessage());
        } catch (RuntimeException failure) {
            LOGGER.severe("Unexpected network command failure: " + failure.getMessage());
            return error("ERROR", "Request could not be completed");
        }
    }

    private static String courses(String[] parts, CampusBackend backend) {
        requireCount(parts, 1, 2);
        List<Course> courses = backend.listCourses(parts.length == 2 ? parts[1] : null);
        StringBuilder response = new StringBuilder("OK|COURSES|").append(courses.size());
        for (Course course : courses) {
            response.append('|').append(encoded(course.courseCode()))
                    .append('|').append(encoded(course.title()))
                    .append('|').append(course.capacity())
                    .append('|').append(course.status());
        }
        return response.toString();
    }

    private static String student(String[] parts, User actor, CampusBackend backend) {
        requireCount(parts, 2, 2);
        requireAuthenticated(actor);
        Student student = backend.getStudent(actor, parts[1]);
        return "OK|STUDENT|" + encoded(student.studentId()) + '|'
                + encoded(student.userId()) + '|' + encoded(student.programme()) + '|'
                + student.yearOfStudy();
    }

    private static String register(String[] parts, User actor, CampusBackend backend, Clock clock) {
        requireCount(parts, 3, 3);
        requireAuthenticated(actor);
        RegistrationAttempt attempt = backend.register(actor, parts[1], parts[2], clock.instant());
        if (attempt.isWaitlisted()) {
            return "OK|WAITLISTED|" + encoded(attempt.waitlistEntry().entryId().toString())
                    + '|' + attempt.waitlistEntry().position();
        }
        Registration registration = attempt.registration();
        return "OK|ENROLLED|" + encoded(registration.registrationId().toString())
                + '|' + encoded(registration.courseCode());
    }

    private static String withdraw(String[] parts, User actor, CampusBackend backend, Clock clock) {
        requireCount(parts, 3, 3);
        requireAuthenticated(actor);
        Registration registration = backend.withdraw(actor, parts[1], parts[2], clock.instant());
        return "OK|WITHDRAWN|" + encoded(registration.registrationId().toString());
    }

    private static void requireAuthenticated(User actor) {
        if (actor == null) {
            throw new SecurityException("Authenticate before issuing this command");
        }
    }

    private static void requireCount(String[] parts, int minimum, int maximum) {
        if (parts.length < minimum || parts.length > maximum
                || Arrays.stream(parts).anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Invalid command arguments");
        }
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String error(String code, String message) {
        String safe = message == null ? "Request failed" : message
                .replace('\r', ' ').replace('\n', ' ').replace('|', '/');
        return "ERR|" + code + '|' + encoded(safe);
    }
}
