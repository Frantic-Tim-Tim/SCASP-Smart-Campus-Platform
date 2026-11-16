# SCASP — Smart Campus Academic Services Platform

## 1. Project Overview

The Smart Campus Academic Services Platform (SCASP) is a Java-based academic services management prototype developed for BoCS3100 – Advanced Java at the National Institute of Public Administration (NIPA).

The platform is intended to centralise academic services, simplify student interactions, improve communication and support administrative operations.

## 2. Project Objectives

- Manage users, students and system roles.
- Provide a centralised course catalogue.
- Support course registration and waiting lists.
- Manage academic service requests.
- Publish campus announcements and notifications.
- Provide administrative reports.
- Maintain an audit trail of system activities.
- Demonstrate Java enterprise, networking, concurrency and distributed computing concepts.
- Provide web and Android client components.

## 3. Technology Stack

| Area | Technology |
|---|---|
| Programming language | Java 21 |
| Build management | Apache Maven |
| Database | MariaDB / MySQL |
| Database access | JDBC |
| Enterprise services | Jakarta EE / EJB |
| Application server | WildFly 42 |
| Web controllers | Jakarta Servlets / JAX-RS |
| Presentation | JSP |
| Remote administration | Java RMI |
| Networking | TCP, UDP and HTTP |
| Mobile development | Android |
| Testing | JUnit 5 |
| Version control | Git and GitHub |

## 4. Project Structure

| Directory | Responsibility |
|---|---|
| `core-domain/` | Domain entities and business rules |
| `concurrent-processing/` | Concurrent task processing |
| `network-gateway/` | TCP, UDP and HTTP communication |
| `enterprise-services/` | Enterprise JavaBeans and business services |
| `persistence-jdbc/` | JDBC repositories and database access |
| `web-application/` | Web application, REST resources, Servlets and JSP |
| `rmi-administration/` | RMI administration client and server |
| `android-client/` | Android application |
| `scasp-ear/` | Enterprise application packaging |
| `database/` | Database scripts |
| `docs/` | Project, architecture and technical documentation |

## 5. Current Project Status

**Status: Development frozen — 16 November 2026**

The project has progressed beyond its initial foundation stage. Core infrastructure and several application components have been implemented. However, the final integrated application has not been fully verified, and some components remain incomplete or have unresolved integration issues.

Development has been paused to preserve the current implementation and prepare the available documentation and demonstration evidence.

The freeze is not an abandonment of the project. Development can resume from the preserved Git branch.

## 6. Preserved Development State

| Item | Value |
|---|---|
| Repository | `Frantic-Tim-Tim/SCASP-Smart-Campus-Platform` |
| Preserved branch | `feat/announcements-notifications` |
| Freeze commit | `1c60bb4` |
| Backup archive | `SCASP-freeze-backup-20261116-034834.tar.gz` |
| Java | 21 |
| Maven | 3.10.0 |
| Application server | WildFly 42.0.0.Final |
| Database server | MariaDB 10.4.32 (XAMPP) |

The freeze branch is published on GitHub and tracks `origin/feat/announcements-notifications`.

## 7. Verified Infrastructure Evidence

The following infrastructure checks were previously successful:

- WildFly application server was started.
- The SCASP enterprise application was deployed.
- The web application's health endpoint returned an UP response.
- The WildFly datasource connection test returned `true`.
- The RMI registry and remote administration service started successfully.
- The networking module recorded 20 passing JUnit tests in its module-level test run.

These results do not establish that every application feature or the complete integrated system is working.

## 8. Known Outstanding Work

- Resolve the RMI-to-EJB remote invocation connection failure.
- Complete and verify end-to-end integration of application features.
- Verify announcements, notifications, service requests and reporting workflows.
- Complete outstanding Android client integration and testing.
- Complete the required CORBA interoperability and migration evidence.
- Perform a fresh full-project build and appropriate integration tests.
- Capture final screenshots and demonstration evidence.

See:
- [Project Freeze Report](docs/PROJECT-FREEZE-REPORT.md)
- [Known Issues](docs/KNOWN-ISSUES.md)
- [Deployment Guide](docs/DEPLOYMENT-GUIDE.md)

## 9. Documentation

The `docs/` directory contains the project charter, requirements catalogue, architecture baseline, networking documentation, risk register, contribution log and freeze records.

## 10. Resuming Development

Before continuing:

1. Clone or open the repository.
2. Check out `feat/announcements-notifications`.
3. Read the project freeze report and known-issues document.
4. Review the current source and environment configuration.
5. Run a fresh Maven build before making changes.
6. Resolve outstanding integration issues in separate branches.
7. Record new test results and update the documentation.

The preserved implementation should be treated as a development snapshot, not as a fully production-ready release.
