# SCASP Networking Module

## 1. Overview

The SCASP Networking Module provides communication capabilities between campus platform components and remote clients. It demonstrates TCP socket communication, UDP datagram communication, and HTTP resource retrieval.

Package: `edu.nipa.scasp.network`

## 2. TCP Communication

TCP provides connection-oriented communication between clients and the SCASP server.

**Classes:**
- `TcpServer` – accepts incoming client connections using `ServerSocket`.
- `TcpClient` – connects to the server using `Socket`.
- `CommandHandler` – validates and processes incoming commands.

**Supported commands:**

| Command | Purpose |
|---|---|
| GET_COURSES | Request the course catalogue |
| REGISTER | Submit a registration command |
| SUBMIT_REQUEST | Submit an academic service request |
| GET_ANNOUNCEMENTS | Request announcements |

The server uses a bounded worker thread pool to handle multiple client connections. Communication uses UTF-8 text, and socket read timeouts help prevent indefinite blocking.

Note: The current command handler returns simulated acknowledgements. Integration with actual SCASP business services is a subsequent task.

### TCP Server Reliability and Security

**Bounded Worker Queue**

The server uses a fixed-size `ThreadPoolExecutor` with an `ArrayBlockingQueue`. This limits both active client-processing threads and queued connections.

When the worker pool and queue are full, additional connections are rejected and their sockets are closed.

**Client Input Validation**

The server enforces a maximum command length of 1,024 characters. Commands exceeding this limit are rejected and the client connection is closed.

**Client Timeout**

Client sockets use a 10-second read timeout to prevent worker threads from remaining blocked indefinitely while waiting for client input.

**Server Lifecycle**

The server follows a one-shot lifecycle:

- A server instance can only be started once.
- Starting an already running server is rejected.
- A closed server instance cannot be restarted.
- Shutdown closes active client connections and stops the worker executor.

**Configuration and Logging**

The server supports configurable bind addresses, worker counts, queue capacity, and an injected `CommandHandler`.

Server events and errors are recorded using Java logging.

## 3. UDP Communication

UDP provides connectionless communication for lightweight messages.

**Classes:**
- `UdpAnnouncementBroadcaster` – sends UDP datagrams.
- `UdpAnnouncementListener` – receives datagrams and handles receive timeouts.

**Use cases:**
- Sending campus announcements.
- Sending server heartbeat messages.
- Detecting the absence of messages within a configured timeout.

UDP does not guarantee message delivery or ordering.

## 4. HTTP Communication

`HttpResourceClient` retrieves resources using HTTP GET requests.

It uses Java's built-in `HttpClient` API and supports HTTP and HTTPS URLs.

Features include:
- Connection timeout.
- Request timeout.
- URL validation.
- HTTP response-status validation.
- Response-body retrieval.

## 5. Testing

The module includes automated JUnit 5 tests.

| Test class | Tests |
|---|---:|
| CommandHandlerTest | 7 |
| TcpCommunicationTest | 2 |
| TcpConcurrentClientTest | 1 |
| TcpServerHardeningTest | 4 |
| UdpCommunicationTest | 3 |
| HttpResourceClientTest | 3 |
| **Total** | **20** |

All 20 tests passed during the module test run, with zero failures, errors, or skipped tests.

The HTTP tests use a local HTTP server. TCP and UDP tests use local communication endpoints.

The hardening tests additionally verify bounded connection handling, oversized command rejection, and server lifecycle restrictions.

## 6. Build and Test

Run the following command from the project root:

`mvn -pl network-gateway -am clean test`

Latest test result:
- Tests run: 20
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: SUCCESS

## 7. Limitations and Future Improvements

- Connect TCP commands to actual SCASP business services.
- Add authentication and authorization to network commands.
- Add message-delivery acknowledgements where reliable UDP delivery is required.
- Add protocol versioning and structured message formats.
- Improve server-side handling of overloaded client connections.
