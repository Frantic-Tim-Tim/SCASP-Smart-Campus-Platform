package edu.nipa.scasp.rmi.api;

import edu.nipa.scasp.rmi.dto.ServerStatus;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines remote administration operations available to SCASP clients.
 */
public interface RemoteAdminService extends Remote {

    /**
     * Checks whether the remote administration server is available.
     */
    String ping() throws RemoteException;

    /**
     * Retrieves the current status of the administration server.
     */
    ServerStatus getServerStatus() throws RemoteException;
}