# Core Domain

Java 21 domain types and registration rules for SCASP. Run the module tests with
`mvn test` from this directory.

## Invariants

- Identifiers and required descriptive fields are non-blank; generated entity IDs
  are immutable UUIDs. A student's `userId` links the student profile to its user.
- A student can have at most one active registration or waitlist entry per course.
- Enrolment never exceeds course capacity. A full open course queues students in
  FIFO order; withdrawing promotes the first waitlisted student and reindexes the
  remaining queue.
- Registration status can move between `ENROLLED` and `WITHDRAWN`; service requests
  follow `SUBMITTED -> IN_PROGRESS -> RESOLVED -> CLOSED`, with cancellation
  permitted before closure.
- Self-service registration and withdrawal require an active `STUDENT` user whose
  ID matches the student profile.
- Course capacity must be positive, and campus coordinates must be valid latitude
  and longitude values.

`RegistrationService` synchronizes its in-memory operations so concurrent calls
through one service instance cannot overbook a course. Persistent deployments
must enforce the same rules in their transaction boundary as well.