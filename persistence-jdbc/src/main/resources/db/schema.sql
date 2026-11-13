CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS students (
    student_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    programme VARCHAR(150) NOT NULL,
    year_of_study INT NOT NULL,
    CONSTRAINT fk_student_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(50) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS registrations (
    registration_id CHAR(36) PRIMARY KEY,
    student_id VARCHAR(100) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_registration_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_registration_course
        FOREIGN KEY (course_code) REFERENCES courses(course_code)
);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    entry_id CHAR(36) PRIMARY KEY,
    student_id VARCHAR(100) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_waitlist_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_waitlist_course
        FOREIGN KEY (course_code) REFERENCES courses(course_code)
);

CREATE TABLE IF NOT EXISTS service_requests (
    request_id CHAR(36) PRIMARY KEY,
    student_id VARCHAR(100) NOT NULL,
    category VARCHAR(150) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_to VARCHAR(150),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_request_student
        FOREIGN KEY (student_id) REFERENCES students(student_id)
);

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id CHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    audience VARCHAR(30) NOT NULL,
    published_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id CHAR(36) PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(150) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    occurred_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS campus_services (
    service_id CHAR(36) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL
);
CREATE TABLE IF NOT EXISTS user_credentials (
    user_id VARCHAR(100) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credentials_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);
