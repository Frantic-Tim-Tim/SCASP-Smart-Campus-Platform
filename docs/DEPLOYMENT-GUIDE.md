# SCASP Deployment and Execution Guide

**Project:** Smart Campus Academic Services Platform (SCASP)
**Course:** BoCS3100 – Advanced Java
**Institution:** National Institute of Public Administration (NIPA)
**Project status:** Development frozen; some features remain incomplete or require integration testing.

This guide explains how to prepare the development environment, configure the database and WildFly, build and deploy SCASP, and verify the available web and RMI components.

> **Important:** SCASP is a partially completed academic project. Successful deployment or a successful health check does not mean every application feature is fully operational. The known limitations in the Project Freeze Report still apply.

---

## 1. Required Software and Versions

The documented development environment is:

| Software | Version / Configuration | Purpose |
|---|---|---|
| Operating system | Windows 10 | Development environment |
| Java Development Kit | Oracle JDK 21 | Compiling and running SCASP |
| Apache Maven | 3.10.0 | Building the multi-module project |
| WildFly | 42.0.0.Final | Jakarta EE application server |
| XAMPP | MariaDB 10.4.32 | Database server |
| MariaDB JDBC driver | 3.5.3 | Java database connectivity |
| Git | Git for Windows | Source-code retrieval and version control |
| Git Bash | Installed with Git for Windows | Running the commands in this guide |
| Web browser | Current Chrome, Firefox or Edge | Accessing the web application |

The paths used in the original development environment included:

- WildFly: `C:\tools\wildfly-42.0.0.Final`
- XAMPP: `C:\xampp`
- Repository: `~/SCASP-Smart-Campus-Platform`

If your software is installed in different locations, replace these paths with your actual installation paths.

### 1.1 Verify Java

Open Git Bash and run:

```bash
java -version
javac -version
```

The Java version should report Java 21.

### 1.2 Verify Maven

```bash
mvn -version
```

Confirm that Maven is available and is using Java 21.

If `mvn` is not recognised, install Maven or add its `bin` directory to the Windows PATH.

### 1.3 Verify Git

```bash
git --version
```

---

## 2. Clone the Repository and Select the Preserved Branch

SCASP source code is hosted at:

`https://github.com/Frantic-Tim-Tim/SCASP-Smart-Campus-Platform`

The preserved development branch is:

`feat/announcements-notifications`

Clone the repository:

```bash
git clone https://github.com/Frantic-Tim-Tim/SCASP-Smart-Campus-Platform.git
```

Enter the project directory:

```bash
cd SCASP-Smart-Campus-Platform
```

Fetch remote branches:

```bash
git fetch origin
```

Check out the preserved branch:

```bash
git switch --track origin/feat/announcements-notifications
```

If the branch already exists locally, use:

```bash
git switch feat/announcements-notifications
git pull
```

Verify the selected branch and latest commit:

```bash
git branch --show-current
git log -1 --oneline
git status
```

The branch should be `feat/announcements-notifications`.

The documentation and development freeze were subsequently recorded in commit `473012a`. If that commit is present, it should appear in the branch history.

---

## 3. Create and Configure the MariaDB Database

SCASP uses MariaDB/MySQL. The database used during development is:

`scasp_db`

The database schema is stored in:

`persistence-jdbc/src/main/resources/db/schema.sql`

### 3.1 Start MariaDB

1. Open the XAMPP Control Panel.
2. Start the MySQL service.
3. Confirm that the service is running.
4. Open phpMyAdmin if you want to inspect the database through a browser.

The default phpMyAdmin address is:

`http://localhost/phpmyadmin`

### 3.2 Create the database

You can create the database through phpMyAdmin or the MariaDB command-line client.

Using the command line from Git Bash:

```bash
"/c/xampp/mysql/bin/mysql.exe" -u root -p
```

Enter the MariaDB root password when prompted.

Execute:

```sql
CREATE DATABASE IF NOT EXISTS scasp_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

SHOW DATABASES;
```

Exit the MariaDB prompt:

```sql
EXIT;
```

### 3.3 Import the SCASP database schema

From the repository root, run:

```bash
"/c/xampp/mysql/bin/mysql.exe" -u root -p scasp_db < persistence-jdbc/src/main/resources/db/schema.sql
```

Enter the database administrator password when prompted.

Verify that the tables were created:

```bash
"/c/xampp/mysql/bin/mysql.exe" -u root -p -D scasp_db -e "SHOW TABLES;"
```

The schema creates tables for the principal application data, including:

- `users`
- `user_credentials`
- `students`
- `courses`
- `registrations`
- `waitlist_entries`
- `service_requests`
- `service_request_updates`
- `announcements`
- `announcement_reads`
- `audit_events`
- `campus_services`

### 3.4 Configure the application database account

WildFly must connect using a MariaDB account with permissions on `scasp_db`.

For a local development environment, an application account can be created as follows. Replace the example password with a strong password of your own.

```sql
CREATE USER 'scasp_app'@'localhost'
IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';

GRANT ALL PRIVILEGES ON scasp_db.*
TO 'scasp_app'@'localhost';

FLUSH PRIVILEGES;
```

If the account already exists, do not create it again. Check its privileges and update them if necessary.

Do not commit database passwords or other credentials to Git.

---

## 4. Configure WildFly and the ScaspDS Datasource

SCASP uses WildFly 42.0.0.Final.

The datasource configuration expected by the application is:

| Setting | Value |
|---|---|
| Datasource name | `ScaspDS` |
| JNDI name | `java:/jdbc/ScaspDS` |
| Database | `scasp_db` |
| Database type | MariaDB |
| JDBC URL | `jdbc:mariadb://localhost:3306/scasp_db` |
| JDBC driver class | `org.mariadb.jdbc.Driver` |
| Database username | `scasp_app` |
| Database password | The password configured in MariaDB |

The EJB repository adapters look up the datasource using `java:/jdbc/ScaspDS`. The JNDI name must match exactly.

### 4.1 Start WildFly

Open another Git Bash terminal and run:

```bash
cd /c/tools/wildfly-42.0.0.Final/bin
./standalone.bat
```

Wait for WildFly to finish starting.

The default management console is:

`http://localhost:9990`

If management authentication is required, use the WildFly management account configured on that server.

### 4.2 Install or register the MariaDB JDBC driver

The project declares MariaDB JDBC driver version 3.5.3.

After Maven has downloaded dependencies, the driver JAR should be available in the local Maven repository, normally at:

`C:\Users\<YOUR_WINDOWS_USER>\.m2\repository\org\mariadb\jdbc\mariadb-java-client\3.5.3\mariadb-java-client-3.5.3.jar`

WildFly must have this driver registered before the datasource can use it.

One option is to deploy the driver JAR through the WildFly management CLI:

```bash
/c/tools/wildfly-42.0.0.Final/bin/jboss-cli.bat --connect --command="deploy C:\Users\<YOUR_WINDOWS_USER>\.m2\repository\org\mariadb\jdbc\mariadb-java-client\3.5.3\mariadb-java-client-3.5.3.jar"
```

Replace `<YOUR_WINDOWS_USER>` with the actual Windows profile directory.

Alternatively, install the driver as a WildFly JDBC module and register it through the management console.

After installation, check the **Configuration → Subsystems → Datasources → JDBC Drivers** area of the WildFly management console. Select the registered MariaDB driver when creating the datasource.

Do not install the same driver repeatedly if it is already registered and working.

### 4.3 Create the datasource

In the WildFly management console:

1. Open `http://localhost:9990`.
2. Navigate to the Datasources section.
3. Add a datasource.
4. Use the name `ScaspDS`.
5. Set the JNDI name to `java:/jdbc/ScaspDS`.
6. Select the registered MariaDB JDBC driver.
7. Set the connection URL to `jdbc:mariadb://localhost:3306/scasp_db`.
8. Enter the MariaDB application username and password.
9. Save the datasource.
10. Enable it if it is not enabled automatically.

The exact menu labels may vary slightly depending on the WildFly console view.

### 4.4 Test the datasource

Use the datasource's **Test Connection** option in the management console.

A successful test should report that the connection succeeded.

You can also inspect the datasource through the WildFly CLI:

```bash
/c/tools/wildfly-42.0.0.Final/bin/jboss-cli.bat --connect
```

At the CLI prompt, run:

```text
/subsystem=datasources/data-source=ScaspDS:test-connection-in-pool
```

A successful result should indicate:

```text
"outcome" => "success"
```

If the datasource test fails, check the database service, driver registration, JDBC URL, username, password and database privileges before deploying the application.

---

## 5. Build the SCASP Project Using Maven

Open Git Bash at the repository root:

```bash
cd ~/SCASP-Smart-Campus-Platform
```

Confirm that the root Maven project file exists:

```bash
ls pom.xml
```

Build the multi-module project:

```bash
mvn clean package
```

This command cleans previous build outputs, compiles the modules, runs configured tests and packages the modules.

The root project uses Java release 21.

### 5.1 Check the build result

A successful Maven build ends with:

```text
BUILD SUCCESS
```

If the build ends with `BUILD FAILURE`, inspect the first compilation or test error in the output. Do not assume that a failed build produced deployable artifacts.

### 5.2 Expected application artifacts

The EAR packaging module is configured to produce:

```text
scasp-ear/target/SCASP.ear
```

The web module is configured to produce:

```text
web-application/target/scasp.war
```

The EAR includes the web application and enterprise modules. For the integrated deployment described in this guide, use the EAR as the primary deployment artifact.

Do not deploy both the EAR and its embedded WAR independently at the same time unless you intentionally want a separate standalone web deployment.

---

## 6. Deploy the EAR/WAR to WildFly

Ensure that:

- MariaDB is running.
- The `scasp_db` database exists.
- The `ScaspDS` datasource is enabled and its connection test succeeds.
- WildFly is running.
- The Maven build completed successfully.

### 6.1 Deploy the enterprise application

The recommended integrated artifact is:

`scasp-ear/target/SCASP.ear`

You can deploy it through the WildFly management console:

1. Open `http://localhost:9990`.
2. Navigate to Deployments.
3. Upload `SCASP.ear`.
4. Enable the deployment.
5. Check that its status is enabled.

Alternatively, use the WildFly CLI from Git Bash:

```bash
/c/tools/wildfly-42.0.0.Final/bin/jboss-cli.bat --connect --command="deploy /c/Users/<YOUR_WINDOWS_USER>/SCASP-Smart-Campus-Platform/scasp-ear/target/SCASP.ear --force"
```

Replace the example Windows user path with the actual location of your cloned repository. If the CLI does not accept the Git Bash-style path, use the equivalent Windows path, for example:

`C:\Users\<YOUR_WINDOWS_USER>\SCASP-Smart-Campus-Platform\scasp-ear\target\SCASP.ear`

### 6.2 Alternative: deploy the WAR separately

The standalone web artifact is:

`web-application/target/scasp.war`

It can be deployed separately for web-module testing:

```bash
/c/tools/wildfly-42.0.0.Final/bin/jboss-cli.bat --connect --command="deploy C:\path\to\SCASP-Smart-Campus-Platform\web-application\target\scasp.war --force"
```

Use this alternative only when testing the WAR independently. Avoid deploying the standalone WAR alongside the EAR if both would use the same `/scasp` context root.

### 6.3 Check deployment status

In the WildFly management console, confirm that the deployment is enabled.

If deployment fails, inspect the WildFly server log:

```text
C:\tools\wildfly-42.0.0.Final\standalone\log\server.log
```

Look for the first deployment exception and its underlying cause.

---

## 7. Start and Test the RMI Administration Service

SCASP includes a Java RMI administration component.

The RMI server uses:

| Component | Port |
|---|---:|
| RMI registry | 1099 |
| Remote administration service | 1100 |
| Registered service name | `SCASPRemoteAdminService` |

The RMI server is separate from the WildFly application server. Start WildFly first if you intend to test operations that call the remote EJB.

### 7.1 Build and install the RMI module dependencies

From the repository root:

```bash
mvn -pl rmi-administration -am install -DskipTests
```

This builds and installs the required project modules into the local Maven repository so that the RMI module's runtime dependencies can be resolved.

### 7.2 Generate the RMI runtime classpath

Run:

```bash
cd rmi-administration
mvn dependency:build-classpath -Dmdep.outputFile=target/rmi-classpath.txt
cd ..
```

The classpath file should now be available at:

`rmi-administration/target/rmi-classpath.txt`

### 7.3 Start the RMI server

In a separate Git Bash terminal, from the repository root, run:

```bash
java -cp "rmi-administration/target/classes;$(cat rmi-administration/target/rmi-classpath.txt)" edu.nipa.scasp.rmi.server.RmiServer
```

The server should report that:

- The RMI registry is running on port 1099.
- `SCASPRemoteAdminService` has been registered.
- The remote service is listening on port 1100.

Keep this terminal open while testing the RMI service.

Press `Ctrl+C` to stop the RMI server.

### 7.4 Test the RMI connection

Open another Git Bash terminal at the repository root:

```bash
java -cp "rmi-administration/target/classes;$(cat rmi-administration/target/rmi-classpath.txt)" edu.nipa.scasp.rmi.client.RmiClient 127.0.0.1 1099
```

The basic connectivity test should report:

- Connected to SCASP RMI Server.
- A successful ping response.
- Server status information.

This confirms that the RMI registry and remote service can communicate.

**Known limitation:** The RMI server's basic ping and status operations are distinct from administrator login and remote EJB operations. The project freeze report records an unresolved RMI-to-EJB invocation problem (`EJBCLIENT000409: No more destinations are available`). Do not interpret a successful ping as proof that all remote administration operations work.

The remote EJB connection uses `remote+http://127.0.0.1:8080` by default. The RMI implementation also accepts the `scasp.ejb.url` Java system property for overriding that endpoint.

### 7.5 Stop the RMI service

Return to the terminal running the RMI server and press:

```text
Ctrl+C
```

---

## 8. Open SCASP and Test the REST Health Endpoint

The web application is packaged with the context root:

`/scasp`

The JAX-RS application uses the API base path:

`/api`

The health resource is:

`/health`

Therefore, the health endpoint is:

`http://localhost:8080/scasp/api/health`

### 8.1 Test using a browser

Open:

`http://localhost:8080/scasp/api/health`

A successful response is:

```json
{
  "status": "UP",
  "application": "SCASP"
}
```

The resource may return the JSON on one line.

### 8.2 Test using curl

From Git Bash:

```bash
curl -i http://localhost:8080/scasp/api/health
```

The response should contain an HTTP success status and the JSON health response.

### 8.3 Open the web application

Open:

`http://localhost:8080/scasp/`

The actual pages and workflows available depend on the deployed build and the features that have been implemented and verified.

Some application pages are protected by role-based security. Authentication may therefore require the WildFly security configuration and valid application accounts.

A successful health endpoint response only confirms that the health resource is responding. It does not prove that every page, database operation or application workflow is working.

---

## 9. Troubleshooting Common Problems

### 9.1 `mvn: command not found`

**Possible cause:** Maven is not installed or its `bin` directory is not in PATH.

**Resolution:**

- Confirm that Maven is installed.
- Add the Maven `bin` directory to Windows PATH.
- Open a new Git Bash terminal.
- Run `mvn -version`.

### 9.2 Maven cannot download dependencies

**Possible causes:**

- No internet connection.
- DNS resolution failure.
- Maven Central is unreachable.
- Network or TLS certificate restrictions.

**Resolution:**

- Check the internet connection.
- Retry the build when Maven Central is reachable.
- Check Maven's error output for the dependency that failed.
- Avoid deleting the local Maven repository unnecessarily.

### 9.3 Java version is incorrect

**Possible cause:** Maven is using a different Java installation from the expected JDK 21.

**Resolution:**

```bash
java -version
javac -version
mvn -version
```

Confirm that all report Java 21. Check `JAVA_HOME` and PATH if they do not.

### 9.4 MariaDB does not start

**Possible causes:**

- Another database process is using port 3306.
- MariaDB has a configuration or data-directory problem.
- The XAMPP MySQL service is already running in another instance.

**Resolution:**

- Inspect the XAMPP Control Panel.
- Check the MariaDB/MySQL error log.
- Check whether port 3306 is already occupied.
- Start only the intended database server.

### 9.5 Database connection is refused

**Possible causes:**

- MariaDB is stopped.
- Incorrect JDBC URL or port.
- Incorrect database name.
- Database account or password is incorrect.

**Resolution:**

Verify:

```text
jdbc:mariadb://localhost:3306/scasp_db
```

Then confirm that the `scasp_app` account can access `scasp_db`.

### 9.6 WildFly cannot find the MariaDB driver

**Possible cause:** The JDBC driver is not registered with WildFly.

**Resolution:**

- Confirm that the MariaDB driver JAR exists.
- Register the driver with WildFly.
- Confirm that it appears under JDBC Drivers.
- Recheck the datasource configuration.

### 9.7 Datasource connection test fails

**Possible causes:**

- Incorrect JNDI name.
- Incorrect driver selection.
- Incorrect JDBC URL.
- Invalid database credentials.
- Missing database privileges.

**Resolution:**

Confirm the exact JNDI name:

```text
java:/jdbc/ScaspDS
```

Confirm that the datasource is enabled and test the connection again.

### 9.8 WildFly deployment fails

**Possible causes:**

- Maven build failed.
- Required datasource is missing.
- A deployment dependency is unavailable.
- A security-domain or application-server configuration is missing.
- An incompatible deployment artifact was selected.

**Resolution:**

- Confirm that `SCASP.ear` was generated by a successful build.
- Confirm that `ScaspDS` exists and works.
- Inspect `standalone/log/server.log`.
- Resolve the first deployment exception before retrying.

The web deployment descriptor references a security domain named `scasp`. If protected pages fail to authenticate, check that the WildFly security configuration provides the required domain and role mapping.

### 9.9 Health endpoint returns HTTP 404

**Possible causes:**

- The EAR/WAR is not deployed.
- The deployment is disabled or failed.
- The context root is incorrect.
- The wrong port is being used.

**Resolution:**

Confirm the deployment is enabled and check:

```text
http://localhost:8080/scasp/api/health
```

### 9.10 RMI registry cannot start

**Possible cause:** Port 1099 or 1100 is already in use.

**Resolution:**

- Stop any previous RMI server instance.
- Check whether another process is using the ports.
- Restart the RMI server after the ports are available.

### 9.11 RMI ping works but administrator operations fail

**Possible cause:** The RMI service is reachable, but the remote EJB connection is unavailable.

**Resolution:**

- Confirm that WildFly is running.
- Confirm that the EAR is deployed.
- Check the remote EJB endpoint.
- Inspect WildFly and RMI logs.
- Review the known `EJBCLIENT000409` issue in the Project Freeze Report.

A successful RMI ping does not establish that remote administrator login or EJB operations are functional.

---

## 10. Recommended Startup Order

For a demonstration or local test, use this order:

1. Start XAMPP MariaDB.
2. Confirm that `scasp_db` exists and the schema has been imported.
3. Start WildFly.
4. Confirm that `ScaspDS` is enabled and its connection test succeeds.
5. Deploy `SCASP.ear` if it is not already deployed.
6. Test the REST health endpoint.
7. Start the RMI server if demonstrating RMI functionality.
8. Run the RMI client ping/status test.
9. Demonstrate only the application workflows that have been verified.

---

## 11. Important Project Limitations

This guide documents how to prepare and start the available SCASP components. It does not imply that all project requirements have been completed.

The following limitations are recorded in the Project Freeze Report:

- Full application end-to-end integration has not been verified.
- RMI-to-EJB remote invocation remains unresolved.
- Android client completion and integration are not verified.
- CORBA implementation and required evidence remain outstanding.
- Some application workflows require further testing.
- A successful health check does not establish that all application features work.

Refer to:

- [Project Freeze Report](PROJECT-FREEZE-REPORT.md)
- [Project Charter](project-charter.md)
- [Architecture Baseline](architecture/architecture-baseline.md)
- [Risk Register](risk-register.md)
- [Contribution Log](contribution-log.md)

---

## 12. Final Verification Checklist

Use this checklist before a local demonstration.

- [ ] Java 21 is installed and active.
- [ ] Maven is installed and active.
- [ ] Correct Git branch is checked out.
- [ ] MariaDB is running.
- [ ] `scasp_db` exists.
- [ ] SCASP database schema has been imported.
- [ ] MariaDB JDBC driver is registered in WildFly.
- [ ] `ScaspDS` is enabled.
- [ ] Datasource connection test succeeds.
- [ ] Maven build completes successfully.
- [ ] `SCASP.ear` is deployed and enabled.
- [ ] REST health endpoint returns the expected JSON.
- [ ] RMI server starts, if required for the demonstration.
- [ ] RMI ping/status test succeeds, if required.
- [ ] Known incomplete features are explained accurately.

**End of deployment guide.**
