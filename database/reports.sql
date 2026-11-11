-- Course capacity, enrolled students and waiting-list totals.
SELECT c.course_code, c.title, c.capacity,
       COUNT(DISTINCT CASE WHEN r.status = 'ENROLLED' THEN r.student_id END) AS enrolled,
       c.capacity - COUNT(DISTINCT CASE WHEN r.status = 'ENROLLED' THEN r.student_id END)
           AS available_seats,
       COUNT(DISTINCT w.entry_id) AS waiting
FROM courses c
LEFT JOIN registrations r ON r.course_code = c.course_code
LEFT JOIN waitlist_entries w ON w.course_code = c.course_code
GROUP BY c.course_code, c.title, c.capacity
ORDER BY c.course_code;

-- Student registration report.
SELECT s.student_id, u.display_name, s.programme, c.course_code, c.title, r.status
FROM students s
JOIN users u ON u.user_id = s.user_id
LEFT JOIN registrations r ON r.student_id = s.student_id
LEFT JOIN courses c ON c.course_code = r.course_code
ORDER BY s.student_id, c.course_code;

-- Service-request workload by assigned staff member and status.
SELECT staff.user_id, staff.display_name, sr.status, sr.priority, COUNT(*) AS request_count
FROM service_requests sr
LEFT JOIN users staff ON staff.user_id = sr.assigned_to
GROUP BY staff.user_id, staff.display_name, sr.status, sr.priority
ORDER BY staff.user_id, sr.status, sr.priority;

-- Audit events joined to the actor's account.
SELECT a.occurred_at, u.user_id, u.display_name, a.action, a.entity_type, a.entity_id
FROM audit_events a
JOIN users u ON u.user_id = a.actor
ORDER BY a.occurred_at DESC;
