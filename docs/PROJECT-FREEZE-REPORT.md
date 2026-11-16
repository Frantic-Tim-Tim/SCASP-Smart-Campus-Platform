# SCASP — Project Development Freeze Report

**Project:** Smart Campus Academic Services Platform (SCASP)
**Course:** BoCS3100 – Advanced Java
**Institution:** National Institute of Public Administration (NIPA)
**Freeze date:** 16 November 2026
**Status:** Development paused; implementation preserved

---

## 1. Purpose of the Freeze

This report records the state of SCASP at the point development was paused.

The purpose is to preserve the current implementation, identify work that remains outstanding, record known technical issues and provide a reliable starting point for future development.

The project has not been abandoned. Development has been paused because the available time for completing and demonstrating the integrated application has run out.

## 2. Source Code Preservation

The current development state was committed and pushed to GitHub.

| Item | Recorded value |
|---|---|
| Repository | `Frantic-Tim-Tim/SCASP-Smart-Campus-Platform` |
| Branch | `feat/announcements-notifications` |
| Freeze commit | `1c60bb4` |
| Commit message | `chore: preserve SCASP development freeze state` |
| Remote branch | `origin/feat/announcements-notifications` |
| Local backup | `SCASP-freeze-backup-20261116-034834.tar.gz` |

The backup archive was validated by listing its contents with `tar -tzf`.

The freeze commit contains the preserved development changes. The local `.vscode/` directory was intentionally excluded from the commit.

## 3. Development Environment

| Component | Version / Configuration |
|---|---|
| Operating system | Windows 10 |
| Java | Oracle Java 21 |
| Maven | Apache Maven 3.10.0 |
| Application server | WildFly 42.0.0.Final |
| Database | MariaDB 10.4.32 through XAMPP |
| Database name | `scasp_db` |
| Database datasource | `ScaspDS` |
| Datasource JNDI | `java:/jdbc/ScaspDS` |
| Database URL | `jdbc:mariadb://localhost:3306/scasp_db` |
| Web context | `/scasp` |
| REST API base | `/scasp/api` |
| RMI registry | Port 1099 |
| RMI service | Port 1100 |

## 4. Architecture and Modules

The repository contains the following major components:

1. `core-domain` — domain objects and business rules.
2. `concurrent-processing` — concurrent task processing.
3. `network-gateway` — TCP, UDP and HTTP communication.
4. `enterprise-services` — EJB-based enterprise services.
5. `persistence-jdbc` — JDBC database access and repositories.
6. `web-application` — web interface, REST resources and Servlets.
7. `rmi-administration` — remote administration.
8. `android-client` — Android client.
9. `scasp-ear` — enterprise application packaging.
10. `database` — database scripts and supporting resources.

## 5. Implementation Progress

The following table distinguishes known implementation work from confirmed end-to-end behaviour.

| Area | Recorded progress | Freeze assessment |
|---|---|---|
| Maven project structure | Multi-module project established | Implemented |
| Core domain | Domain objects, roles, statuses and service logic developed | Implemented; full integration not reverified |
| Concurrent processing | Concurrent task processing developed | Implemented; full integration not reverified |
| Networking | TCP, UDP and HTTP components with automated tests | Module test evidence recorded |
| JDBC persistence | Repositories, datasource support and schema resources developed | Implemented; full application verification outstanding |
| Enterprise services | EJB services developed and deployed during earlier integration work | Implemented; end-to-end verification outstanding |
| User management | User-management services and role-based operations developed | Implemented; full workflow verification outstanding |
| Web application | WAR, REST resources, Servlets and JSP functionality developed | Partially verified |
| Announcements and notifications | Domain, service, persistence and web components developed | Integration and workflow verification outstanding |
| Service requests | Domain, service, persistence and web components developed | Integration and workflow verification outstanding |
| Academic reports | Reporting service and web components developed | Integration and workflow verification outstanding |
| RMI administration | Registry and remote service startup confirmed | Remote EJB invocation unresolved |
| Android client | Project directory and client component present | Completion and integration not verified |
| CORBA | Interoperability and migration requirements identified | Required implementation/evidence outstanding |
| Documentation | Initial project and technical documents available | Freeze documentation being added |

The presence of source files alone does not prove that a complete feature works. Features marked as requiring verification must be tested before being described as complete.

## 6. Confirmed Infrastructure Checks

The following checks succeeded during development:

### 6.1 Web Health Endpoint

The endpoint:

`http://localhost:8080/scasp/api/health`

returned:

`{"status":"UP","application":"SCASP"}`

This confirmed that the deployed web application's health resource was responding at that time.

### 6.2 Database Connectivity

The WildFly datasource connection test returned:

`[true]`

Datasource details:

- Name: `ScaspDS`
- JNDI: `java:/jdbc/ScaspDS`
- Database: `scasp_db`

### 6.3 RMI Server Startup

The RMI server successfully reported:

- RMI registry running on port 1099.
- Remote administration service registered as `SCASPRemoteAdminService`.
- Remote service listening on port 1100.

This confirms server startup and registration, not successful completion of remote administrative operations.

### 6.4 Networking Tests

The networking module documentation records a successful module test run:

- Tests run: 20
- Failures: 0
- Errors: 0
- Skipped: 0

This is a module-level result and should not be interpreted as a successful full-project integration test.

## 7. Outstanding Technical Work

The most significant unresolved integration issue is the RMI-to-EJB remote invocation failure.

The remote administration client can start its RMI service, but its attempt to invoke the remote EJB fails with:

`EJBCLIENT000409: No more destinations are available`

The observed failure occurs during remote EJB invocation. It has not been established as an incorrect-password failure.

Other outstanding work includes:

- Verify the latest full Maven build.
- Verify the complete web application workflows.
- Verify announcements and notification delivery.
- Verify service-request creation, assignment and status updates.
- Verify academic report generation.
- Complete RMI-to-EJB integration.
- Verify Android client functionality and backend communication.
- Complete CORBA migration documentation and any required demonstration.
- Conduct end-to-end and security testing.
- Capture genuine screenshots and test evidence.

## 8. Freeze Rules

While the project remains frozen:

1. Do not delete or overwrite the preserved source code.
2. Do not force-push over the preserved branch.
3. Do not merge unverified changes into `main`.
4. Keep the backup archive in a safe location.
5. Do not describe unverified functionality as fully operational.
6. Record new findings in the documentation when development resumes.

## 9. Resumption Procedure

When development resumes:

1. Open the repository.
2. Check out `feat/announcements-notifications`.
3. Verify the current commit and working-tree status.
4. Read `KNOWN-ISSUES.md`.
5. Read `DEPLOYMENT-GUIDE.md`.
6. Start MariaDB and WildFly using the documented environment.
7. Run a fresh Maven build.
8. Verify the health endpoint and datasource.
9. Address the RMI-to-EJB connection issue.
10. Test each major application workflow.
11. Update this report with new test evidence.

## 10. Final Freeze Assessment

SCASP has a substantial implementation foundation, including a modular Maven structure, domain and persistence components, enterprise services, web resources, networking functionality and remote administration infrastructure.

However, the complete integrated application has not been verified. Outstanding integration, testing, mobile and CORBA work must be addressed before claiming that all project requirements have been fulfilled.

**Final status:** Development paused and source code preserved.


## 11. Team Participation and Development Resources

SCASP was a collaborative group project involving Thomas, Majid and
Wendy. All three members participated in brainstorming, discussing
proposed solutions and shaping the project's concept, objectives and
overall direction.

During implementation, the group encountered computer-resource
limitations. Majid and Wendy did not have suitable computers or laptops
available to independently develop and test the application in their own
development environments.

Consequently, Thomas carried out much of the practical coding,
environment configuration, integration and technical testing. This
reflected the available development resources and does not mean that
Majid and Wendy were excluded from the group's conceptualisation and
planning contributions.

The experience highlights the importance of assessing equipment
availability and development resources when planning collaborative
software projects.
