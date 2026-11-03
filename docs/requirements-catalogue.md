# SCASP — Requirements Catalogue

**Project:** Smart Campus Academic Services Platform (SCASP)  
**Course:** BoCS3100 — Advanced Java  
**Institution:** National Institute of Public Administration (NIPA)  
**Version:** 1.0  
**Date:** 3 November 2026

---

## 1. Introduction

This document defines the functional and non-functional requirements for the Smart Campus Academic Services Platform (SCASP).

The platform will provide an integrated environment through which students can access academic services, while academic and administrative staff can manage courses, process service requests, publish announcements and monitor institutional activities.

The requirements in this document will guide the system's design, implementation and testing.

## 2. Intended Users

The system will support the following user categories:

1. **Students:** View courses, register for courses, withdraw from courses, submit academic service requests and view announcements.
2. **Academic Staff:** Manage course information, monitor registration and process academic service requests.
3. **Administrative Staff:** Manage announcements, view reports and monitor institutional activities.
4. **System Administrators:** Manage user accounts, roles and system operations.

## 3. Functional Requirements

Functional requirements describe the services and operations that the system must provide.

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | The system shall allow users to log in using registered credentials. | High |
| FR-02 | The system shall provide role-based access for students, academic staff, administrative staff and administrators. | High |
| FR-03 | The system shall allow authorized staff to create, update and manage course information. | High |
| FR-04 | The system shall allow students to view and search the available course catalogue. | High |
| FR-05 | The system shall allow students to register for available courses. | High |
| FR-06 | The system shall prevent a student from registering for the same course more than once. | High |
| FR-07 | The system shall prevent course registrations from exceeding the available course capacity. | High |
| FR-08 | The system shall allow students to withdraw from registered courses. | High |
| FR-09 | The system shall maintain a waiting list when a course reaches its registration capacity. | High |
| FR-10 | The system shall allow students to submit academic service requests. | High |
| FR-11 | The system shall allow authorized staff to assign, prioritize and update academic service requests. | High |
| FR-12 | The system shall allow students to track the status of their submitted service requests. | High |
| FR-13 | The system shall allow authorized staff to publish announcements for intended audiences. | Medium |
| FR-14 | The system shall generate reports on course capacity, waiting lists, student registrations and service-request workloads. | High |
| FR-15 | The system shall maintain an audit trail of important system activities, including the user, action, affected record and time. | High |

### 3.1 Functional Requirement Acceptance Criteria

The following conditions will be used to verify important system functions:

- **FR-01:** A user with valid credentials can log in; invalid credentials are rejected.
- **FR-02:** A student cannot access administrative functions without the required role.
- **FR-03:** Authorized staff can create and update a course, and the changes are reflected in the catalogue.
- **FR-04:** A student can search for courses and view relevant course information.
- **FR-05:** A student can register for a course that has available capacity.
- **FR-06:** Attempting to register for an already registered course is rejected.
- **FR-07:** A course cannot have more active registrations than its configured capacity.
- **FR-08:** A student can withdraw from a course and the registration status is updated.
- **FR-09:** When a course is full, an eligible student can be added to its waiting list.
- **FR-10:** A student can submit a service request and receive a request reference.
- **FR-11:** Authorized staff can assign a request and change its priority or status.
- **FR-12:** A student can view the current status of their own service requests.
- **FR-13:** Authorized staff can publish an announcement that can be retrieved by its intended audience.
- **FR-14:** Authorized staff can generate reports containing the relevant system records.
- **FR-15:** Important operations produce audit records containing the actor, action, entity and timestamp.

## 4. Non-Functional Requirements

Non-functional requirements describe the quality attributes and operating standards expected of the system.

| ID | Requirement | Priority |
|---|---|---|
| NFR-01 | **Performance:** The system should respond to ordinary user requests within three seconds under normal demonstration conditions. | High |
| NFR-02 | **Security:** The system shall enforce role-based authorization for protected operations. | High |
| NFR-03 | **Data Integrity:** The system shall prevent duplicate registrations and enforce course-capacity constraints, including during concurrent requests. | High |
| NFR-04 | **Reliability:** The system shall handle invalid input and operational errors without unexpectedly terminating the application. | High |
| NFR-05 | **Maintainability:** The system shall use modular components with clearly defined responsibilities. | High |
| NFR-06 | **Usability:** The user interfaces shall provide clear navigation, understandable messages and validation feedback. | Medium |
| NFR-07 | **Compatibility:** The web application shall work with a modern web browser, while the mobile client shall be developed for the Android platform. | Medium |
| NFR-08 | **Scalability:** The processing and networking components shall support multiple users and concurrent requests. | High |
| NFR-09 | **Portability:** The Java application shall be buildable using the documented Java and Maven configuration. | Medium |
| NFR-10 | **Auditability:** Important operations shall be traceable through stored audit records. | High |

### 4.1 Non-Functional Requirement Verification

- **NFR-01:** Measure response times for selected operations during testing.
- **NFR-02:** Attempt protected operations using accounts with insufficient permissions.
- **NFR-03:** Test duplicate registrations and simultaneous attempts to register for a course with limited capacity.
- **NFR-04:** Submit invalid data and simulate selected failures to verify controlled error handling.
- **NFR-05:** Inspect the project structure to confirm separation of domain, persistence, networking and presentation responsibilities.
- **NFR-06:** Conduct a basic usability test with representative users.
- **NFR-07:** Test the web interface in a modern browser and run the mobile application on an Android emulator or device.
- **NFR-08:** Run tests involving multiple concurrent clients or tasks.
- **NFR-09:** Build the project using the documented setup instructions on the team members' computers.
- **NFR-10:** Perform selected operations and verify that the corresponding audit records are stored.

## 5. Business Rules and Constraints

The following rules govern important system operations:

1. A student must be authenticated before accessing protected academic services.
2. A user may only perform operations permitted by their assigned role.
3. A student must not have duplicate active registrations for the same course.
4. Active course registrations must not exceed the course's configured capacity.
5. Students may be placed on a waiting list when a course is full.
6. Registration, withdrawal and waiting-list changes must preserve data consistency.
7. Important administrative and academic operations must be recorded in the audit trail.
8. Only authorized staff may manage courses and process academic service requests.

## 6. Assumptions

- The system will initially use demonstration accounts and sample institutional data.
- The project will not integrate with a real university student-information system.
- The platform will use a MySQL database for persistent storage.
- The Android client will communicate with the platform through a network-based interface.
- The system will demonstrate the technologies specified in the Advanced Java assignment.

## 7. Requirements Traceability

Each requirement will be linked to its implementation component and corresponding test evidence during development.

| Requirement Area | Main Implementation Component |
|---|---|
| User management and roles | Web application and enterprise services |
| Course catalogue and registration | Core domain, Collections and JDBC |
| Waiting-list management | Core domain and concurrent processing |
| Academic service requests | Enterprise services and JDBC |
| Announcements and notifications | Networking and enterprise services |
| Reports and audit trail | JDBC persistence |
| Web access | Servlets and JSP |
| Distributed administration | Java RMI |
| Legacy interoperability analysis | CORBA migration documentation |
| Mobile access and campus services | Android client |

## 8. Approval

This requirements catalogue establishes the initial functional and non-functional requirements for SCASP. Changes to requirements during implementation should be documented and agreed upon by the project team.

**Prepared by:** SCASP Project Team  
**Team Members:** Thomas, Majid and Mukuka  
**Status:** Initial requirements baseline