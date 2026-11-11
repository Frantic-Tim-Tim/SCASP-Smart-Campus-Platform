INSERT INTO users (user_id, display_name, role, status)
VALUES ('demo-student-001', 'Demo Student One', 'STUDENT', 'ACTIVE')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO users (user_id, display_name, role, status)
VALUES ('demo-staff-001', 'Demo Academic Staff', 'STAFF', 'ACTIVE')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO students (student_id, user_id, programme, year_of_study)
VALUES ('DEMO-2026-001', 'demo-student-001', 'Information Systems', 1)
ON DUPLICATE KEY UPDATE programme = VALUES(programme);

INSERT INTO courses (course_code, title, capacity, status)
VALUES ('BACS3100', 'Advanced Java Programming', 40, 'OPEN')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO courses (course_code, title, capacity, status)
VALUES ('BACS2100', 'Database Systems', 35, 'OPEN')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO campus_services (service_id, name, description, latitude, longitude)
VALUES ('00000000-0000-4000-8000-000000000001', 'Main Library',
        'Fictional demonstration campus library', -15.4145, 28.2773)
ON DUPLICATE KEY UPDATE name = VALUES(name);
