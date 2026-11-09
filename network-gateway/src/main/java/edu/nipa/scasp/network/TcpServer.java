package edu.nipa.scasp.network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpServer implements AutoCloseable {

    private static final Logger LOGGER =
            Logger.getLogger(TcpServer.class.getName());

    private static final int CLIENT_TIMEOUT = 10_000;
    private static final int MAX_COMMAND_LENGTH = 1_024;
    private static final int SHUTDOWN_TIMEOUT = 5;

    private final int port;
    private final InetAddress bindAddress;
    private final CommandHandler commandHandler;
    private final ThreadPoolExecutor clientExecutor;

    /*
     * Tracks every accepted client socket, including clients
     * waiting in the executor queue.
     */
    private final Set<Socket> activeClients =
            ConcurrentHashMap.newKeySet();

    private volatile ServerSocket serverSocket;
    private volatile Thread serverThread;
    private volatile boolean running;

    private boolean started;
    private boolean closed;

    /**
     * Maintains compatibility with existing code.
     * Uses a queue capacity equal to the worker count.
     */
    public TcpServer(int port, int workerThreads) {

        this(
                null,
                port,
                workerThreads,
                workerThreads,
                new CommandHandler()
        );
    }

    /**
     * Allows the worker queue capacity to be configured.
     */
    public TcpServer(
            int port,
            int workerThreads,
            int queueCapacity
    ) {

        this(
                null,
                port,
                workerThreads,
                queueCapacity,
                new CommandHandler()
        );
    }

    /**
     * Allows configuration of the bind address,
     * worker threads, queue capacity and command handler.
     */
    public TcpServer(
            InetAddress bindAddress,
            int port,
            int workerThreads,
            int queueCapacity,
            CommandHandler commandHandler
    ) {

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "Invalid port number"
            );
        }

        if (workerThreads < 1) {
            throw new IllegalArgumentException(
                    "At least one worker thread is required"
            );
        }

        if (queueCapacity < 1) {
            throw new IllegalArgumentException(
                    "Queue capacity must be at least one"
            );
        }

        if (commandHandler == null) {
            throw new IllegalArgumentException(
                    "CommandHandler cannot be null"
            );
        }

        this.port = port;
        this.bindAddress = bindAddress;
        this.commandHandler = commandHandler;

        /*
         * Fixed number of workers and a bounded waiting queue.
         * AbortPolicy rejects new tasks when capacity is exhausted.
         */
        this.clientExecutor = new ThreadPoolExecutor(
                workerThreads,
                workerThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Starts the TCP server.
     *
     * This server instance has a one-shot lifecycle.
     * It cannot be restarted after it has been closed.
     */
    public synchronized void start() throws IOException {

        if (closed) {
            throw new IllegalStateException(
                    "A closed server cannot be restarted"
            );
        }

        if (started) {
            throw new IllegalStateException(
                    "Server has already been started"
            );
        }

        ServerSocket socket = new ServerSocket();

        try {

            InetSocketAddress address =
                    bindAddress == null
                            ? new InetSocketAddress(port)
                            : new InetSocketAddress(bindAddress, port);

            socket.bind(address);

            serverSocket = socket;
            running = true;
            started = true;

            serverThread = new Thread(
                    this::acceptConnections,
                    "scasp-tcp-server"
            );

            serverThread.start();

            LOGGER.info(
                    "SCASP TCP Server listening on "
                            + socket.getLocalSocketAddress()
            );

        } catch (IOException | RuntimeException exception) {

            running = false;
            socket.close();

            throw exception;
        }
    }

    /**
     * Accepts incoming client connections and submits
     * them to the bounded worker executor.
     */
    private void acceptConnections() {

        while (running) {

            Socket clientSocket = null;

            try {

                clientSocket = serverSocket.accept();

                /*
                 * Register the socket immediately after accepting it.
                 * This includes sockets waiting in the executor queue.
                 */
                activeClients.add(clientSocket);

                /*
                 * Handle a shutdown that occurred while accept()
                 * was returning a newly accepted socket.
                 */
                if (!running) {

                    activeClients.remove(clientSocket);
                    closeSocket(clientSocket);

                    break;
                }

                LOGGER.info(
                        "Client connected: "
                                + clientSocket.getRemoteSocketAddress()
                );

                Socket acceptedSocket = clientSocket;

                try {

                    clientExecutor.execute(
                            () -> handleClient(acceptedSocket)
                    );

                } catch (RejectedExecutionException exception) {

                    LOGGER.warning(
                            "Server is busy; rejecting client connection"
                    );

                    activeClients.remove(clientSocket);
                    closeSocket(clientSocket);
                }

            } catch (SocketException exception) {

                if (running) {

                    LOGGER.log(
                            Level.WARNING,
                            "Server socket error",
                            exception
                    );
                }

            } catch (IOException exception) {

                if (running) {

                    LOGGER.log(
                            Level.WARNING,
                            "Error accepting client connection",
                            exception
                    );
                }

                if (clientSocket != null) {

                    activeClients.remove(clientSocket);
                    closeSocket(clientSocket);
                }
            }
        }

        LOGGER.info("TCP accept thread stopped");
    }

    /**
     * Handles communication with an individual client.
     */
    private void handleClient(Socket clientSocket) {

        try (
                Socket socket = clientSocket;

                PushbackReader reader = new PushbackReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                        ),
                        1
                );

                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            socket.setSoTimeout(CLIENT_TIMEOUT);

            String command;

            while ((command = readCommand(reader)) != null) {

                String response =
                        commandHandler.handleCommand(command);

                writer.write(response);
                writer.newLine();
                writer.flush();
            }

        } catch (SocketTimeoutException exception) {

            LOGGER.info(
                    "Client disconnected after idle timeout: "
                            + clientSocket.getRemoteSocketAddress()
            );

        } catch (IOException exception) {

            LOGGER.log(
                    Level.FINE,
                    "Client communication ended",
                    exception
            );

        } finally {

            /*
             * Remove the client whether communication succeeds,
             * fails, times out or is interrupted by shutdown.
             */
            activeClients.remove(clientSocket);
        }
    }

    /**
     * Reads one command while enforcing the maximum
     * allowed command length.
     *
     * Supports LF, CRLF and CR line endings.
     */
    private String readCommand(PushbackReader reader)
            throws IOException {

        StringBuilder command = new StringBuilder();

        boolean receivedCharacter = false;

        int character;

        while ((character = reader.read()) != -1) {

            receivedCharacter = true;

            if (character == '\n') {
                return command.toString();
            }

            if (character == '\r') {

                int nextCharacter = reader.read();

                if (nextCharacter != -1 && nextCharacter != '\n') {
                    reader.unread(nextCharacter);
                }

                return command.toString();
            }

            if (command.length() >= MAX_COMMAND_LENGTH) {

                throw new IOException(
                        "Command exceeds maximum allowed length of "
                                + MAX_COMMAND_LENGTH + " characters"
                );
            }

            command.append((char) character);
        }

        /*
         * Process the final command if the client disconnects
         * without sending a line terminator.
         */
        if (receivedCharacter) {
            return command.toString();
        }

        return null;
    }

    /**
     * Safely closes an individual client socket.
     */
    private void closeSocket(Socket socket) {

        try {

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException exception) {

            LOGGER.log(
                    Level.WARNING,
                    "Error closing client socket",
                    exception
            );
        }
    }

    /**
     * Returns the actual port on which the server is listening.
     */
    public int getPort() {

        ServerSocket socket = serverSocket;

        if (socket == null || !started) {

            throw new IllegalStateException(
                    "Server has not started"
            );
        }

        return socket.getLocalPort();
    }

    /**
     * Returns the number of active or queued client sockets.
     */
    public int getConnectedClientCount() {

        return activeClients.size();
    }

    /**
     * Returns the number of tasks waiting for worker threads.
     */
    public int getQueuedClientCount() {

        return clientExecutor.getQueue().size();
    }

    /**
     * Returns the number of currently executing client tasks.
     */
    public int getActiveWorkerCount() {

        return clientExecutor.getActiveCount();
    }

    /**
     * Stops the server and releases its resources.
     *
     * Closes the listening socket, active clients and queued
     * client sockets. The executor cannot be restarted.
     */
    @Override
    public void close() {

        ServerSocket socketToClose;
        Thread threadToJoin;

        synchronized (this) {

            if (closed) {
                return;
            }

            closed = true;
            running = false;

            socketToClose = serverSocket;
            threadToJoin = serverThread;
        }

        /*
         * Stop accepting new connections.
         */
        if (socketToClose != null) {

            try {

                socketToClose.close();

            } catch (IOException exception) {

                LOGGER.log(
                        Level.WARNING,
                        "Error closing server socket",
                        exception
                );
            }
        }

        /*
         * Close every registered client socket.
         * This includes queued clients that have not yet
         * started executing.
         */
        for (Socket client : activeClients) {

            closeSocket(client);
        }

        /*
         * Stop queued and running worker tasks.
         */
        clientExecutor.shutdownNow();

        try {

            if (!clientExecutor.awaitTermination(
                    SHUTDOWN_TIMEOUT,
                    TimeUnit.SECONDS
            )) {

                LOGGER.warning(
                        "Client workers did not terminate in time"
                );
            }

            /*
             * Wait for the accept thread to finish.
             */
            if (threadToJoin != null
                    && threadToJoin != Thread.currentThread()) {

                threadToJoin.join(
                        SHUTDOWN_TIMEOUT * 1000L
                );
            }

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            LOGGER.warning(
                    "Server shutdown was interrupted"
            );
        }

        LOGGER.info("SCASP TCP Server stopped");
    }
}
