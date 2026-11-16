# SCASP — Project Charter

## 1. Project Information

**Project Name:** Smart Campus Academic Services Platform (SCASP)

**Course:** BoCS3100 – Advanced Java

**Institution:** National Institute of Public Administration (NIPA)

**Project Team:** Thomas, Majid and Wendy

**Project Start Date:** October 2026

**Submission Deadline:** November 2026, 00:00

**Project Type:** Academic Software Development Project

## 2. Background and Problem Statement

Higher education institutions provide various academic and administrative services to students and staff. These services include course registration, academic enquiries, announcements, student requests and access to campus facilities.

When these services are handled through disconnected processes, students may experience difficulties accessing information and tracking their requests, while administrative staff may face challenges managing registrations, requests, waiting lists and reporting.

The Smart Campus Academic Services Platform (SCASP) is proposed as an integrated software solution that brings these services together through a centralised platform.

## 3. Project Aim

To design, develop, test and demonstrate an integrated Java-based academic services platform that simplifies access to academic and campus services while demonstrating the major concepts covered in the Advanced Java course.

## 4. Project Objectives

The project aims to:

1. Develop a user and role management system for students and staff.
2. Provide a centralised course catalogue.
3. Implement course registration and waiting-list management.
4. Allow students to submit and track academic service requests.
5. Enable staff to publish and manage announcements.
6. Generate notifications for important system events.
7. Provide administrative reports and workload monitoring.
8. Maintain an audit trail of important system activities.
9. Provide campus service information and location access.
10. Demonstrate Java Collections, multithreading, networking, EJB, JDBC, Servlets, JSP, RMI, CORBA concepts and Android development.

## 5. Intended Users and Stakeholders

### Students
Students will use the platform to:
- View available courses.
- Register for courses and withdraw registrations.
- Join course waiting lists.
- Submit academic service requests.
- View announcements and notifications.
- Locate selected campus services.

### Academic and Administrative Staff
Staff will use the platform to:
- Manage course information and capacity.
- Process and assign academic service requests.
- Publish announcements.
- Monitor registrations and waiting lists.
- Generate administrative reports.

### Project Team
The development team consists of:
- Thomas
- Majid
- Wendy

The team is responsible for requirements analysis, design, implementation, testing, integration and documentation.

## 6. Project Scope

### In Scope
- User and role management.
- Course catalogue management.
- Course registration and waiting lists.
- Academic service requests.
- Announcements and notifications.
- Reporting and audit records.
- Campus service information and locations.
- Web-based access using Servlets and JSP.
- Remote administration using Java RMI.
- Android mobile application.
- Networking and concurrent processing.
- Database persistence using JDBC.
- Enterprise services using EJB.
- CORBA interoperability analysis and migration design.

### Out of Scope
- Integration with actual university financial systems.
- Processing real student payments.
- Integration with live national education databases.
- Deployment as a production university-wide system.
- Use of real student personal information.

## 7. Proposed Technology Stack

| Area | Technology |
|---|---|
| Programming language | Java 21 |
| Build management | Maven |
| Database | MySQL |
| Database connectivity | JDBC |
| Enterprise services | Jakarta EE / EJB |
| Web controllers | Jakarta Servlets |
| Presentation | JSP / Jakarta Pages |
| Remote communication | Java RMI |
| Legacy interoperability | CORBA analysis and design |
| Mobile development | Android Studio |
| Version control | Git and GitHub |
| Testing | JUnit 5 |

## 8. Initial Team Responsibilities

| Member | Initial Role | Responsibilities |
|---|---|---|
| Thomas | Architecture and Integration Lead | Repository management, architecture, domain model and integration |
| Majid | Backend and Data Lead | Collections, concurrency, networking, EJB, JDBC and database |
| Wendy | Client, Quality and Documentation Lead | Web and mobile interfaces, testing and user documentation |

These were the initial planned responsibilities. Actual implementation was affected by the computer-resource constraints documented in Section 13.6.

## 9. Project Deliverables

The expected deliverables include:

1. Project charter and requirements catalogue.
2. System architecture and domain model diagrams.
3. Java domain and collections implementation.
4. Concurrent processing module.
5. TCP/UDP networking module.
6. Enterprise services module.
7. JDBC persistence layer and database scripts.
8. Servlet and JSP web application.
9. RMI administration client and server.
10. CORBA interoperability and migration dossier.
11. Android mobile application.
12. Automated and manual test evidence.
13. Technical documentation and user guide.
14. Final integrated demonstration.

## 10. Constraints and Assumptions

### Constraints
- The project must be completed within the allocated academic deadline.
- The development team consists of three students.
- The system must demonstrate all required Advanced Java topics.
- Development must use the agreed technologies and shared repository.
- All members must be able to explain their contributions.

### Assumptions
- All three team members are available throughout the development period.
- It was initially assumed that each member would have access to a suitable computer and development environment. This assumption was not met during implementation.
- The team can access the required development tools and software.
- The platform will initially use fictional demonstration data.

## 11. Success Criteria

The project will be considered successful when:

1. The required academic services are implemented in a working prototype.
2. The required Advanced Java concepts are demonstrated.
3. Course registration and waiting-list rules work correctly.
4. Concurrent operations do not violate course capacity.
5. Database transactions preserve data consistency.
6. Web and mobile interfaces demonstrate the required workflows.
7. Remote administration and CORBA migration are demonstrated or documented as required.
8. The application passes the required tests.
9. All three members demonstrate meaningful contributions.
10. The final system can be built, executed and demonstrated using the project documentation.

## 12. Approval

This charter establishes the initial project direction and scope. Changes to the scope will be documented and must retain coverage of the required course topics.

**Prepared by:** SCASP Development Team

**Team Members:** Thomas, Majid and Wendy

**Project Status:** Development frozen – 16 November 2026
---

# 13. Actual Development History and Project Outcome

**Updated:** 16 November 2026

The original charter established the project scope and intended team responsibilities. During implementation, the project progressed from initial repository setup into a multi-module Java enterprise application.

### 13.1 Development Approach

The project initially followed a collaborative team structure. During the later implementation phase, Thomas took responsibility for coordinating and carrying out the remaining development, integration, testing and documentation work individually.

The implementation included:

- Establishment of a multi-module Maven project.
- Development of the core domain model.
- Development of concurrent-processing functionality.
- Implementation of TCP, UDP and HTTP networking components.
- Development of JDBC repositories and database connectivity.
- Enterprise JavaBeans and application-server integration.
- Web application development using Servlets, JSP and REST resources.
- Remote administration using Java RMI.
- Development of user-management, announcements, service-request and reporting components.

### 13.2 Actual Development Environment

The implementation used Java 21, Maven 3.10.0, MariaDB through XAMPP, WildFly 42.0.0.Final, Git and GitHub.

The application was organized into separate Maven modules, including `core-domain`, `concurrent-processing`, `network-gateway`, `enterprise-services`, `persistence-jdbc`, `web-application`, `rmi-administration` and `scasp-ear`.

### 13.3 Project Outcome

The project achieved a substantial implementation foundation. Infrastructure checks confirmed that the web health endpoint responded, the WildFly datasource connected successfully, and the RMI registry and remote service could start.

The networking module also had a recorded successful test run.

However, the complete integrated application was not fully verified. The RMI-to-EJB remote invocation remained unresolved, and several application workflows required further integration testing.

### 13.4 Development Freeze

On 16 November 2026, development was paused to preserve the available implementation and prepare project documentation.

The preserved source state is:

- **Branch:** `feat/announcements-notifications`
- **Commit:** `1c60bb4`
- **Commit message:** `chore: preserve SCASP development freeze state`
- **Remote:** `origin/feat/announcements-notifications`

A separate backup archive was also created:

`SCASP-freeze-backup-20261116-034834.tar.gz`

The freeze is a development pause, not project abandonment.

### 13.5 Revised Success Assessment

The original success criteria remain the intended project requirements. They should not be treated as all achieved merely because implementation files exist.

Final assessment requires:

1. A successful fresh full-project build.
2. Verification of the principal student and staff workflows.
3. Verification of role-based access and persistence.
4. Successful integration testing of remote administration.
5. Completion or appropriate documentation of the CORBA requirements.
6. Android client verification.
7. Final demonstration and test evidence.

**Current project status:** Development paused; source code preserved.

### 13.6 Actual Team Participation and Resource Constraints

The project was a collaborative group effort involving Thomas, Majid and
Wendy. All three members participated in brainstorming, discussing proposed
solutions and shaping the project's concept, objectives and direction.

The original team responsibilities documented in this charter represented
the planned division of work. During implementation, the group encountered
computer-resource limitations. Majid and Wendy did not have suitable
computers or laptops available to independently develop and test the
application in their own development environments.

As a result, Thomas carried out much of the practical coding, environment
configuration, integration and technical testing. This arrangement arose
from the resource limitations and does not mean that the other members
were excluded from the group's conceptualisation and planning contributions.
