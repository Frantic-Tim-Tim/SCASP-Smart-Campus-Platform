package edu.nipa.scasp.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class NetworkDiagnostics {
    private NetworkDiagnostics() {
    }

    public static InetAddress localHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }
}
