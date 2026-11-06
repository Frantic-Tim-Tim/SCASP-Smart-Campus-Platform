package edu.nipa.scasp.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TcpServer implements AutoCloseable {

    private final int port;
    private final CommandHandler commandHandler;
    private final ExecutorService clientExecutor;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running;

    public TcpServer(int port, int workerThreads) {

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number");
        }

        if (workerThreads < 1) {
            throw new IllegalArgumentException(
                    "At least one worker thread is required"
            );
        }

        this.port = port;
        this.commandHandler = new CommandHandler();
        this.clientExecutor =
                Executors.newFixedThreadPool(workerThreads);
    }

    public synchronized void start() throws IOException {

        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        serverSocket = new ServerSocket(port);
        running = true;

        serverThread = new Thread(
                this::acceptConnections,
                "scasp-tcp-server"
        );

        serverThread.start();

        System.out.println(
                "SCASP TCP Server listening on port "
                        + serverSocket.getLocalPort()
        );
    }

    private void acceptConnections() {

        while (running) {

            try {
                Socket clientSocket = serverSocket.accept();

                System.out.println(
                        "Client connected: "
                                + clientSocket.getRemoteSocketAddress()
                );

                clientExecutor.execute(
                        () -> handleClient(clientSocket)
                );

            } catch (SocketException exception) {

                if (running) {
                    System.err.println(
                            "Socket error: " + exception.getMessage()
                    );
                }

            } catch (IOException exception) {

                if (running) {
                    System.err.println(
                            "Connection error: " + exception.getMessage()
                    );
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {

        try (
                Socket socket = clientSocket;

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            socket.setSoTimeout(10000);

            String command;

            while ((command = reader.readLine()) != null) {

                String response =
                        commandHandler.handleCommand(command);

                writer.write(response);
                writer.newLine();
                writer.flush();
            }

        } catch (IOException exception) {

            System.err.println(
                    "Client communication ended: "
                            + exception.getMessage()
            );
        }
    }

    public int getPort() {

        if (serverSocket == null) {
            throw new IllegalStateException("Server has not started");
        }

        return serverSocket.getLocalPort();
    }

    @Override
    public synchronized void close() {

        running = false;

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            System.err.println(
                    "Error closing server: " + exception.getMessage()
            );
        }

        clientExecutor.shutdownNow();

        try {
            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Client workers did not terminate in time");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}