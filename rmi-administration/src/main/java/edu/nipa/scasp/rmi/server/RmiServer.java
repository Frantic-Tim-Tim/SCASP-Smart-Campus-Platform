package edu.nipa.scasp.rmi.server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Starts and manages the SCASP RMI Registry and remote service.
 */
public class RmiServer implements AutoCloseable {

    public static final String SERVICE_NAME =
            "SCASPRemoteAdminService";

    private final int registryPort;
    private final int servicePort;

    private Registry registry;
    private RemoteAdminServiceImpl service;
    private boolean started;

    public RmiServer(int registryPort, int servicePort) {

        if (registryPort < 1 || registryPort > 65535) {
            throw new IllegalArgumentException(
                    "Invalid RMI registry port"
            );
        }

        if (servicePort < 1 || servicePort > 65535) {
            throw new IllegalArgumentException(
                    "Invalid RMI service port"
            );
        }

        if (registryPort == servicePort) {
            throw new IllegalArgumentException(
                    "Registry and service ports must be different"
            );
        }

        this.registryPort = registryPort;
        this.servicePort = servicePort;
    }

    /**
     * Creates the registry and registers the remote service.
     */
    public synchronized void start() throws RemoteException {

        if (started) {
            throw new IllegalStateException(
                    "RMI server has already started"
            );
        }

        Registry newRegistry = LocateRegistry.createRegistry(
                registryPort
        );

        RemoteAdminServiceImpl newService = null;

        try {
            newService = new RemoteAdminServiceImpl(servicePort);

            newRegistry.rebind(SERVICE_NAME, newService);

            registry = newRegistry;
            service = newService;
            started = true;

            System.out.println(
                    "SCASP RMI Registry running on port "
                            + registryPort
            );

            System.out.println(
                    "Remote administration service registered as "
                            + SERVICE_NAME
            );

            System.out.println(
                    "Remote service listening on port "
                            + servicePort
            );

        } catch (RemoteException exception) {

            if (newService != null) {
                UnicastRemoteObject.unexportObject(
                        newService, true
                );
            }

            UnicastRemoteObject.unexportObject(
                    newRegistry, true
            );

            throw exception;
        }
    }

    /**
     * Stops the service and releases RMI resources.
     */
    @Override
    public synchronized void close() {

        if (!started) {
            return;
        }

        try {
            registry.unbind(SERVICE_NAME);
        } catch (RemoteException | NotBoundException exception) {
            System.err.println(
                    "Could not unbind RMI service: "
                            + exception.getMessage()
            );
        }

        try {
            UnicastRemoteObject.unexportObject(service, true);
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (RemoteException exception) {
            System.err.println(
                    "Error shutting down RMI server: "
                            + exception.getMessage()
            );
        }

        started = false;
        service = null;
        registry = null;

        System.out.println("SCASP RMI Server stopped.");
    }

    public static void main(String[] args) throws Exception {

        int registryPort = 1099;
        int servicePort = 1100;

        RmiServer server = new RmiServer(
                registryPort,
                servicePort
        );

        server.start();

        Runtime.getRuntime().addShutdownHook(
                new Thread(server::close)
        );

        System.out.println(
                "Press Ctrl+C to stop the RMI server."
        );
    }
}