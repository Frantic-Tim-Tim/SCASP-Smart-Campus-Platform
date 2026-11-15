# SCASP — Project Charter

## 1. Project Information

**Project Name:** Smart Campus Academic Services Platform (SCASP)

**Course:** BoCS3100 – Advanced Java

**Institution:** National Institute of Public Administration (NIPA)

**Project Team:** Thomas, Majid and Mukuka

**Project Start Date:** October 2026

**Submission Deadline:** 8 November 2026, 00:00

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

## 5. Intended Users, Stakeholders and Project Team

### 5.1 Students
Students will use the platform to:
- View available courses.
- Register for courses and withdraw registrations.
- Join course waiting lists.
- Submit academic service requests.
- View announcements and notifications.
- Locate selected campus services.

### 5.2 Academic and Administrative Staff
Staff will use the platform to:
- Manage course information and capacity.
- Process and assign academic service requests.
- Publish announcements.
- Monitor registrations and waiting lists.
- Generate administrative reports.

### 5.3 System Administrators
System administrators will:
- Manage user accounts and roles.
- Monitor system operations.
- Maintain administrative access and security.

### 5.4 Project Team

The SCASP project is developed by three members:
- Thomas
- Majid
- Mukuka

All members share responsibility for requirements analysis,
design, implementation, testing, integration and documentation.

Project contributions are recorded according to the actual work
performed by each member. GitHub commit authorship may not always
identify the individual contributor because some team members
used a shared GitHub account when experiencing account-related
technical difficulties.

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

## 8. Team Responsibilities and Collaboration

The three project members share responsibility for delivering the
Smart Campus Academic Services Platform.

The members collaborate throughout the development process and
may work together on the same modules, features and technical
tasks.

Contributions include direct implementation, technical discussions,
solution recommendations, development guidance, code reviews,
testing, debugging and integration support.

Thomas primarily handles direct code implementation and repository
management, while Majid and Mukuka contribute through technical
guidance, solution discussions, testing and reviewing implemented
features.

Team responsibilities rotate after Stages 3 and 7, as required
by the project assignment. All members are expected to contribute
to the development process and understand the complete system.

Git and GitHub are used to manage source code, branches, commits
and pull requests. The team maintains shared responsibility for
ensuring that all project modules work together as one integrated
system.

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
- Each member has access to a suitable computer and development environment.
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

**Team Members:** Thomas, Majid and Mukuka

**Project Status:** Stage 0 – Project Inception