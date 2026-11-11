package edu.nipa.scasp.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.net.URLEncoder;

public final class TcpCampusClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;

    public TcpCampusClient(String host, int port, int timeoutMillis) throws IOException {
        Objects.requireNonNull(host, "host");
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                    StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
                    StandardCharsets.UTF_8));
        } catch (IOException failure) {
            socket.close();
            throw failure;
        }
    }

    public String authenticate(String userId, char[] password) throws IOException {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(password, "password");
        try {
            return exchange("AUTH|" + URLEncoder.encode(userId, StandardCharsets.UTF_8) + "|"
                    + URLEncoder.encode(new String(password), StandardCharsets.UTF_8));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public synchronized String request(String command) throws IOException {
        if (command == null || command.isBlank() || command.indexOf('\n') >= 0
                || command.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("command must contain one non-empty line");
        }
        return exchange(command);
    }

    private synchronized String exchange(String message) throws IOException {
        output.write(message);
        output.newLine();
        output.flush();
        String response = input.readLine();
        if (response == null) {
            throw new IOException("Server closed the connection");
        }
        return response;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
