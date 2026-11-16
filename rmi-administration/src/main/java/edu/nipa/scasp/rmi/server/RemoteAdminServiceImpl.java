package edu.nipa.scasp.rmi.server;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.rmi.api.RemoteAdminService;
import edu.nipa.scasp.rmi.dto.ServerStatus;
import edu.nipa.scasp.enterprise.service.RemoteUserManagementService;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteAdminServiceImpl extends UnicastRemoteObject
        implements RemoteAdminService {

    private static final String EJB_LOOKUP =
            "ejb:/SCASP/com.scasp-enterprise-services-1.0-SNAPSHOT/"
            + "RemoteUserManagementBean!"
            + "edu.nipa.scasp.enterprise.service.RemoteUserManagementService";

    private final RmiSessionManager sessionManager = new RmiSessionManager();

    private final Map<String, AdminConnection> connections =
            new ConcurrentHashMap<>();

    public RemoteAdminServiceImpl(int exportPort) throws RemoteException {
        super(exportPort);
    }

    @Override
    public String ping() {
        return "SCASP RMI Server is running!";
    }

    @Override
    public ServerStatus getServerStatus() {
        return new ServerStatus(
                "SCASP Remote Administration Server",
                "RUNNING",
                Instant.now()
        );
    }

    @Override
    public String login(String username, String password)
            throws RemoteException {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new SecurityException("Username and password are required.");
        }

        InitialContext context = null;

        try {
            Hashtable<String, Object> environment = new Hashtable<>();

            environment.put(
                    Context.INITIAL_CONTEXT_FACTORY,
                    "org.wildfly.naming.client.WildFlyInitialContextFactory"
            );

            environment.put(
                    Context.PROVIDER_URL,
                    System.getProperty(
                            "scasp.ejb.url",
                            "remote+http://127.0.0.1:8080"
                    )
            );

            environment.put(Context.SECURITY_PRINCIPAL, username);
            environment.put(Context.SECURITY_CREDENTIALS, password);
            environment.put("jboss.naming.client.ejb.context", "true");

            context = new InitialContext(environment);

            RemoteUserManagementService ejb =
                    (RemoteUserManagementService) context.lookup(EJB_LOOKUP);

            // The protected EJB verifies the authenticated caller's ADMIN role.
            // No caller-supplied User object is trusted as an identity.
            ejb.getAllUsers();

            String token = sessionManager.createSession(username);

            connections.put(token, new AdminConnection(context, ejb));

            context = null;
            return token;

        } catch (NamingException | RuntimeException exception) {
            throw new SecurityException(
                    "Administrator authentication failed: "
                            + exception.getMessage(),
                    exception
            );
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                }
            }
        }
    }

    @Override
    public void logout(String sessionToken) {
        sessionManager.invalidateSession(sessionToken);

        AdminConnection connection = connections.remove(sessionToken);

        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public User registerUser(String token, User newUser) {
        return connection(token).service().registerUser(newUser);
    }

    @Override
    public User getUserById(String token, String userId) {
        return connection(token).service().getUserById(userId);
    }

    @Override
    public List<User> getAllUsers(String token) {
        return connection(token).service().getAllUsers();
    }

    @Override
    public User changeUserRole(
            String token, String userId, UserRole newRole) {
        return connection(token).service().changeUserRole(userId, newRole);
    }

    @Override
    public User suspendUser(String token, String userId) {
        return connection(token).service().suspendUser(userId);
    }

    @Override
    public User activateUser(String token, String userId) {
        return connection(token).service().activateUser(userId);
    }

    private AdminConnection connection(String token) {
        sessionManager.requireAdministrator(token);

        AdminConnection connection = connections.get(token);

        if (connection == null) {
            throw new SecurityException("Administrator session is unavailable.");
        }

        return connection;
    }

    private record AdminConnection(
            InitialContext context,
            RemoteUserManagementService service) {

        private void close() {
            try {
                context.close();
            } catch (NamingException ignored) {
            }
        }
    }
}
