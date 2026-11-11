CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    password_salt VARBINARY(32),
    password_hash VARBINARY(64),
    CONSTRAINT chk_users_role CHECK (role IN
        ('STUDENT', 'STAFF', 'ADMIN', 'ACADEMIC_STAFF', 'ADMINISTRATIVE_STAFF', 'SYSTEM_ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_user_password_pair CHECK
        ((password_salt IS NULL AND password_hash IS NULL)
            OR (password_salt IS NOT NULL AND password_hash IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS students (
    student_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    programme VARCHAR(160) NOT NULL,
    year_of_study INT NOT NULL,
    CONSTRAINT chk_student_year CHECK (year_of_study > 0),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(32) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT chk_course_capacity CHECK (capacity > 0),
    CONSTRAINT chk_course_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS registrations (
    registration_id CHAR(36) NOT NULL UNIQUE,
    student_id VARCHAR(64) NOT NULL,
    course_code VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (student_id, course_code),
    CONSTRAINT chk_registration_status CHECK (status IN ('ENROLLED', 'WITHDRAWN')),
    CONSTRAINT fk_registration_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_registration_course FOREIGN KEY (course_code) REFERENCES courses(course_code),
    INDEX idx_registration_course_status (course_code, status)
);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    entry_id CHAR(36) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    course_code VARCHAR(32) NOT NULL,
    position INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_waitlist_student_course UNIQUE (student_id, course_code),
    CONSTRAINT chk_waitlist_position CHECK (position > 0),
    CONSTRAINT fk_waitlist_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_waitlist_course FOREIGN KEY (course_code) REFERENCES courses(course_code),
    INDEX idx_waitlist_course_position (course_code, position)
);

CREATE TABLE IF NOT EXISTS service_requests (
    request_id CHAR(36) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    category VARCHAR(100) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    assigned_to VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_request_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT chk_request_status CHECK
        (status IN ('SUBMITTED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT fk_request_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_request_assignee FOREIGN KEY (assigned_to) REFERENCES users(user_id),
    INDEX idx_request_status_priority (status, priority, created_at)
);

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id CHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    audience VARCHAR(16) NOT NULL,
    published_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_announcement_audience CHECK (audience IN ('ALL', 'STUDENTS', 'STAFF')),
    INDEX idx_announcement_audience_date (audience, published_at)
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id CHAR(36) PRIMARY KEY,
    actor VARCHAR(64) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor) REFERENCES users(user_id),
    INDEX idx_audit_entity_date (entity_type, entity_id, occurred_at)
);

CREATE TABLE IF NOT EXISTS campus_services (
    service_id CHAR(36) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    CONSTRAINT chk_service_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_service_longitude CHECK (longitude BETWEEN -180 AND 180)
);
