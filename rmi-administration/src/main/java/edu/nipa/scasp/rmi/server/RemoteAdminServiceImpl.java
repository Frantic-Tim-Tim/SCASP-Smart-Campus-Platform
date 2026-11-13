package edu.nipa.scasp.rmi.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;

import edu.nipa.scasp.rmi.api.RemoteAdminService;
import edu.nipa.scasp.rmi.dto.ServerStatus;

/**
 * Server-side implementation of SCASP remote administration.
 */
public class RemoteAdminServiceImpl extends UnicastRemoteObject
        implements RemoteAdminService {

    private static final long serialVersionUID = 1L;

    private static final String SERVER_NAME =
            "SCASP Remote Administration Server";

    public RemoteAdminServiceImpl(int servicePort)
            throws RemoteException {
        super(servicePort);
    }

    @Override
    public String ping() throws RemoteException {
        return "SCASP RMI Server is running!";
    }

    @Override
    public ServerStatus getServerStatus() throws RemoteException {
        return new ServerStatus(
                SERVER_NAME,
                "RUNNING",
                Instant.now()
        );
    }
}
