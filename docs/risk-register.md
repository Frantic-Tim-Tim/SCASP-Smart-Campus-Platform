# SCASP — Project Risk Register

**Project:** Smart Campus Academic Services Platform (SCASP)  
**Course:** BoCS3100 — Advanced Java  
**Institution:** National Institute of Public Administration (NIPA)  
**Version:** 1.0  
**Date:** 3 November 2026  
**Project Deadline:** November 2026, 00:00

---

## 1. Introduction

This risk register identifies potential threats that may affect the successful development, integration, testing and delivery of the Smart Campus Academic Services Platform.

It records each risk, its possible impact, likelihood, severity, mitigation strategy and initially planned risk owner.

The project team will review and update this register throughout development.

## 2. Risk Assessment Method

Each risk is assessed using two factors:

- **Likelihood:** The probability that the risk will occur.
- **Impact:** The seriousness of the consequences if the risk occurs.

Risk levels are classified as Low, Medium or High.

## 3. Identified Project Risks

| ID | Risk | Likelihood | Impact | Level | Mitigation Strategy | Planned Risk Owner |
|---|---|---|---|---|---|---|
| R-01 | Integration failures between independently developed components. | High | High | High | Define interfaces early, integrate components regularly and test communication between modules. | Thomas |
| R-02 | Difficulty configuring or deploying Enterprise JavaBeans and the Jakarta EE runtime. | Medium | High | High | Verify the runtime and Java compatibility early and create a small deployment test before implementing the full service layer. | Majid |
| R-03 | Obsolete or unavailable CORBA tools and libraries. | High | Medium | High | Prepare the required IDL, interoperability analysis and migration design. Confirm whether an ORB environment is available before attempting a full implementation. | Thomas |
| R-04 | Delays caused by the short project development schedule. | High | High | High | Break work into daily deliverables, prioritize assignment requirements and track progress through GitHub Issues. | All |
| R-05 | Team members becoming unavailable or failing to complete assigned tasks. | Medium | High | High | Assign clear responsibilities, maintain regular communication and document work so other members can support unfinished tasks. | All |
| R-06 | Database connection or transaction failures. | Medium | High | High | Test database connectivity early, use prepared statements, implement transactions and maintain database scripts. | Majid |
| R-07 | Concurrent registration operations causing duplicate records or exceeding course capacity. | Medium | High | High | Use appropriate synchronization and database transaction controls, then test simultaneous registration attempts. | Majid |
| R-08 | Network communication failures between clients and services. | Medium | Medium | Medium | Implement timeouts, input validation, exception handling and tests for disconnected clients. | Majid |
| R-09 | Web and Android clients failing to communicate with backend services. | Medium | High | High | Define API operations and data formats early and test them using a simple client before completing the interfaces. | Wendy |
| R-10 | Incomplete automated testing and insufficient evidence of correctness. | Medium | High | High | Write tests alongside implementation, retain test results and perform integration testing before submission. | Wendy |
| R-11 | Loss of source code or accidental overwriting of another member's work. | Low | High | Medium | Use Git branches, meaningful commits, pull requests and regular remote backups. | Thomas |
| R-12 | Security weaknesses such as unauthorized access or exposed credentials. | Medium | High | High | Enforce role checks, hash passwords, validate input and keep credentials out of the repository. | All |
| R-13 | Differences in development environments between team members. | Medium | Medium | Medium | Standardize Java and Maven versions, document setup instructions and test the project on all team computers. | Thomas |
| R-14 | Mobile application development taking longer than expected. | High | Medium | High | Prioritize the required Android screens and core API communication before optional improvements. | Wendy |
| R-15 | Insufficient documentation or demonstration evidence. | Medium | Medium | Medium | Maintain documentation, screenshots, test results and contribution records throughout development. | Wendy |

## 4. Risk Response Priorities

The team will prioritize the following risks:

### Priority 1: Integration failures (R-01)

Integration problems could prevent the individual technologies from working together as one platform.

**Response:** Establish interfaces early and integrate components continuously rather than waiting until the final day.

### Priority 2: Schedule delays (R-04)

The project has a limited development period and requires several different technologies.

**Response:** Prioritize essential assignment requirements, assign daily deliverables and monitor progress.

### Priority 3: Enterprise JavaBeans deployment (R-02)

Incorrect runtime configuration could prevent the enterprise service component from running.

**Response:** Verify the development environment with a small EJB deployment before implementing the full functionality.

### Priority 4: Concurrent registration errors (R-07)

Simultaneous registration requests could cause data inconsistency.

**Response:** Combine appropriate Java concurrency controls with database-level integrity and transaction mechanisms.

### Priority 5: CORBA technology limitations (R-03)

CORBA modules are not included in modern standard JDK distributions.

**Response:** Focus on the required interoperability analysis, IDL design and migration strategy. Investigate an available ORB only if the assignment environment supports it.

## 5. Risk Monitoring

The project team will:

1. Review the risk register during team progress meetings.
2. Update risk levels when circumstances change.
3. Record newly identified risks.
4. Assign an owner to every significant risk.
5. Verify that mitigation actions are being implemented.
6. Record any risks that actually occur and the actions taken to resolve them.

## 6. Conclusion

The risk register provides a structured approach to identifying and managing challenges that could affect SCASP.

By identifying risks early, assigning responsibility and implementing mitigation strategies, the team aims to reduce project delays, improve integration and deliver a reliable demonstration of the required Advanced Java technologies.

**Prepared by:** SCASP Project Team  
**Status:** Initial risk baseline
---

# 7. Development Freeze Risk Review

**Updated:** 16 November 2026

The following review records risks that remained relevant at the point development was paused.

| ID | Updated risk | Likelihood | Impact | Level | Response |
|---|---|---|---|---|---|
| R-16 | RMI client cannot obtain an available remote EJB destination. | High | High | High | Investigate the HTTPS remoting connector, EJB client configuration, deployment naming and server logs. |
| R-17 | Latest source changes may contain build or integration failures not yet detected by a fresh full build. | Medium | High | High | Run a fresh full Maven build and resolve failures module by module. |
| R-18 | Announcements, notifications, service requests and reporting may not work as complete end-to-end workflows. | High | High | High | Perform workflow-based integration testing and retain evidence. |
| R-19 | Android client may not be fully integrated with backend services. | High | Medium | High | Verify the client, API endpoints and network communication. |
| R-20 | Required CORBA evidence may be incomplete. | High | Medium | High | Review assignment requirements and complete the required IDL, interoperability and migration evidence. |
| R-21 | Final demonstration evidence may be insufficient. | High | Medium | High | Capture genuine screenshots, test outputs and deployment evidence. |
| R-22 | A single developer carrying the remaining implementation workload may face time and continuity constraints. | High | High | High | Preserve the source branch, maintain clear documentation and resume work in small verified increments. |

## 8. Risks Mitigated During Development

The following earlier risks were partially addressed through implementation:

- **R-02 — Enterprise deployment:** WildFly was configured and the SCASP enterprise application was deployed during development.
- **R-06 — Database connectivity:** The MariaDB datasource was configured and its connection test returned `true`.
- **R-08 — Network reliability:** The networking module includes timeouts, bounded worker handling, input validation and automated tests.
- **R-11 — Source preservation:** A dedicated freeze commit was pushed to a remote Git branch and a separate backup archive was created.
- **R-13 — Environment configuration:** Java, Maven, WildFly and database configuration details were recorded in the deployment documentation.

These mitigations do not mean that all associated risks have been eliminated.

## 9. Freeze Risk Management

While development is paused:

1. Keep the preserved branch and backup archive intact.
2. Avoid unverified changes to the preserved source.
3. Do not merge unfinished features into the protected main branch.
4. Keep credentials and local environment secrets out of the public repository.
5. Maintain the known-issues register.
6. Record new test evidence when development resumes.

## 10. Final Risk Assessment

The most significant remaining risks are incomplete end-to-end integration, remote EJB connectivity, unverified mobile functionality, outstanding CORBA deliverables and insufficient final demonstration evidence.

The source preservation measures reduce the risk of losing the current implementation, but they do not resolve outstanding technical risks.

**Overall assessment at freeze:** High residual project risk until integration and acceptance testing are completed.

## 11. Actual Team Resource Constraint

During implementation, the project experienced a development-resource
constraint: not all group members had suitable computers or laptops for
independent development and testing.

**Impact:** The planned distribution of practical implementation tasks
could not be fully carried out. Much of the coding, integration and technical
testing was consequently handled by Thomas.

**Response and lesson:** Future group projects should establish access to
suitable development equipment early, assess each member's working
environment and plan shared development arrangements where necessary.

This constraint did not remove the collaborative contributions of Thomas,
Majid and Wendy during brainstorming and project conceptualisation.
