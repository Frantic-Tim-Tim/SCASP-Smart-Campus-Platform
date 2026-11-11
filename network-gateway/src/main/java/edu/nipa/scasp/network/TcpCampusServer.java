package edu.nipa.scasp.network;

import edu.nipa.scasp.domain.CampusBackend;
import edu.nipa.scasp.domain.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.net.URLDecoder;

public final class TcpCampusServer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(TcpCampusServer.class.getName());
    private final InetAddress bindAddress;
    private final int requestedPort;
    private final int clientTimeoutMillis;
    private final CampusBackend backend;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<Socket> activeClients = ConcurrentHashMap.newKeySet();
    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    public TcpCampusServer(InetAddress bindAddress, int port, int clientTimeoutMillis,
                           CampusBackend backend, Clock clock) {
        this.bindAddress = Objects.requireNonNull(bindAddress, "bindAddress");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (clientTimeoutMillis < 1) {
            throw new IllegalArgumentException("clientTimeoutMillis must be positive");
        }
        requestedPort = port;
        this.clientTimeoutMillis = clientTimeoutMillis;
        this.backend = Objects.requireNonNull(backend, "backend");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized void start() throws IOException {
        if (closed.get() || !started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server can only be started once");
        }
        running.set(true);
        ServerSocket socket = new ServerSocket();
        try {
            socket.bind(new InetSocketAddress(bindAddress, requestedPort));
            socket.setSoTimeout(250);
            serverSocket = socket;
            acceptThread = Thread.ofPlatform().name("scasp-tcp-accept").start(this::acceptClients);
        } catch (IOException | RuntimeException failure) {
            running.set(false);
            socket.close();
            clients.shutdownNow();
            throw failure;
        }
    }

    public int port() {
        ServerSocket socket = serverSocket;
        if (socket == null || !socket.isBound()) {
            throw new IllegalStateException("Server is not running");
        }
        return socket.getLocalPort();
    }

    private void acceptClients() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                activeClients.add(client);
                try {
                    clients.execute(() -> {
                        try {
                            serve(client);
                        } finally {
                            activeClients.remove(client);
                        }
                    });
                } catch (java.util.concurrent.RejectedExecutionException rejected) {
                    activeClients.remove(client);
                    client.close();
                }
            } catch (SocketTimeoutException ignored) {
                // Periodically observe the shutdown flag.
            } catch (SocketException closed) {
                if (running.get()) {
                    running.set(false);
                    LOGGER.severe("TCP accept loop stopped: " + closed.getMessage());
                }
            } catch (IOException failure) {
                if (running.get()) {
                    running.set(false);
                    LOGGER.severe("TCP accept loop stopped: " + failure.getMessage());
                }
            }
        }
    }

    private void serve(Socket client) {
        try (client) {
            client.setSoTimeout(clientTimeoutMillis);
            try (InputStream input = client.getInputStream();
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                         client.getOutputStream(), StandardCharsets.UTF_8))) {
                User actor = null;
                while (running.get()) {
                    String line;
                    try {
                        line = readLine(input);
                    } catch (SocketTimeoutException timeout) {
                        write(output, NetworkProtocol.error("TIMEOUT", "Client timed out"));
                        return;
                    } catch (LineTooLongException tooLong) {
                        write(output, NetworkProtocol.error("INVALID", "Message is too long"));
                        return;
                    } catch (CharacterCodingException malformed) {
                        write(output, NetworkProtocol.error("INVALID", "Message is not valid UTF-8"));
                        return;
                    }
                    if (line == null) {
                        return;
                    }
                    if (actor == null) {
                        actor = authenticate(line, output);
                    } else {
                        write(output, NetworkProtocol.handle(line, actor, backend, clock));
                    }
                }
            }
        } catch (IOException ignored) {
            // A disconnected client must not stop the accept loop.
        }
    }

    private User authenticate(String line, BufferedWriter output) throws IOException {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 3 || !fields[0].equals("AUTH")
                || fields[1].isBlank() || fields[2].isEmpty()) {
            write(output, NetworkProtocol.error("UNAUTHENTICATED",
                    "Send AUTH|userId|password before commands"));
            return null;
        }
        char[] password = new char[0];
        try {
            String userId = URLDecoder.decode(fields[1], StandardCharsets.UTF_8);
            password = URLDecoder.decode(fields[2], StandardCharsets.UTF_8).toCharArray();
            User actor = backend.authenticate(userId, password);
            write(output, "OK|AUTHENTICATED");
            return actor;
        } catch (SecurityException | IllegalArgumentException failure) {
            write(output, NetworkProtocol.error("UNAUTHENTICATED", "Invalid credentials"));
            return null;
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int next;
        while ((next = input.read()) != -1) {
            if (next == '\n') {
                byte[] line = bytes.toByteArray();
                int length = line.length;
                if (length > 0 && line[length - 1] == '\r') {
                    length--;
                }
                try {
                    return StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(line, 0, length)).toString();
                } catch (CharacterCodingException malformed) {
                    throw malformed;
                }
            }
            if (bytes.size() >= NetworkProtocol.MAX_LINE_BYTES) {
                throw new LineTooLongException();
            }
            bytes.write(next);
        }
        if (bytes.size() == 0) {
            return null;
        }
        throw new IOException("Client disconnected before completing a framed message");
    }

    private static void write(BufferedWriter output, String response) throws IOException {
        output.write(response);
        output.newLine();
        output.flush();
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        running.set(false);
        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException failure) {
                clients.shutdownNow();
                throw new IllegalStateException("Could not close the server socket", failure);
            }
        }
        for (Socket client : activeClients) {
            try {
                client.close();
            } catch (IOException failure) {
                LOGGER.warning("Could not close a connected client: " + failure.getMessage());
            }
        }
        clients.shutdownNow();
        try {
            if (!clients.awaitTermination(2, TimeUnit.SECONDS)) {
                LOGGER.warning("Some TCP client handlers did not terminate promptly");
            }
            if (acceptThread != null) {
                acceptThread.join(2_000);
                if (acceptThread.isAlive()) {
                    LOGGER.warning("TCP accept thread did not terminate promptly");
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class LineTooLongException extends IOException {
    }
}
