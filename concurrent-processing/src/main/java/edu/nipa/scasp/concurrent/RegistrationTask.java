package edu.nipa.scasp.concurrent;

import edu.nipa.scasp.domain.CampusBackend;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.User;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;

public final class RegistrationTask implements Callable<RegistrationAttempt>, Runnable {
    private final CampusBackend backend;
    private final User actor;
    private final String studentId;
    private final String courseCode;
    private final Instant submittedAt;

    public RegistrationTask(CampusBackend backend, User actor, String studentId,
                            String courseCode, Instant submittedAt) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.actor = Objects.requireNonNull(actor, "actor");
        this.studentId = Objects.requireNonNull(studentId, "studentId");
        this.courseCode = Objects.requireNonNull(courseCode, "courseCode");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
    }

    @Override
    public RegistrationAttempt call() {
        return backend.register(actor, studentId, courseCode, submittedAt);
    }

    @Override
    public void run() {
        call();
    }
}
