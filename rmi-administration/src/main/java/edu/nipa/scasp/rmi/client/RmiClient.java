
package edu.nipa.scasp.rmi.client;

import edu.nipa.scasp.rmi.api.RemoteAdminService;
import edu.nipa.scasp.rmi.dto.ServerStatus;
import edu.nipa.scasp.rmi.server.RmiServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Client used to communicate with the SCASP remote administration server.
 */
public class RmiClient {

    private final RemoteAdminService remoteService;

    /**
     * Connects to the RMI registry and looks up the remote service.
     */
    public RmiClient(String host, int registryPort)
            throws RemoteException, NotBoundException {

        Registry registry = LocateRegistry.getRegistry(
                host,
                registryPort
        );

        this.remoteService = (RemoteAdminService)
                registry.lookup(RmiServer.SERVICE_NAME);
    }

    /**
     * Checks whether the remote server is responding.
     */
    public String ping() throws RemoteException {
        return remoteService.ping();
    }

    /**
     * Retrieves the remote server's status.
     */
    public ServerStatus getServerStatus() throws RemoteException {
        return remoteService.getServerStatus();
    }

    /**
     * Runs a simple demonstration of the RMI client.
     */
    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : "localhost";
        int registryPort = args.length > 1
                ? Integer.parseInt(args[1])
                : 1099;

        try {
            RmiClient client = new RmiClient(host, registryPort);

            System.out.println("Connected to SCASP RMI Server.");
            System.out.println();

            System.out.println("PING RESPONSE:");
            System.out.println(client.ping());

            System.out.println();
            System.out.println("SERVER STATUS:");

            ServerStatus status = client.getServerStatus();

            System.out.println("Server: " + status.serverName());
            System.out.println("Status: " + status.status());
            System.out.println("Checked at: " + status.checkedAt());

        } catch (RemoteException | NotBoundException exception) {

            System.err.println(
                    "Could not communicate with SCASP RMI Server."
            );
            System.err.println("Reason: " + exception.getMessage());

        } catch (NumberFormatException exception) {

            System.err.println(
                    "Invalid registry port. Supply a valid port number."
            );
        }
    }
}