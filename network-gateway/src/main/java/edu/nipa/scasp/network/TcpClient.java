package edu.nipa.scasp.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClient implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public TcpClient(String host, int port) throws IOException {

        socket = new Socket();

        // Attempt connection within five seconds.
        socket.connect(
                new InetSocketAddress(host, port),
                5000
        );

        // Wait at most ten seconds for a server response.
        socket.setSoTimeout(10000);

        reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        writer = new BufferedWriter(
                new OutputStreamWriter(
                        socket.getOutputStream(),
                        StandardCharsets.UTF_8
                )
        );
    }

    public String sendCommand(String command) throws IOException {

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException(
                    "Command cannot be empty"
            );
        }

        writer.write(command);
        writer.newLine();
        writer.flush();

        String response = reader.readLine();

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