package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.CampusService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcCampusServiceRepository
        implements edu.nipa.scasp.domain.Repository<UUID, CampusService> {
    private final DataSource dataSource;

    public JdbcCampusServiceRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<CampusService> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM campus_services WHERE service_id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find campus service", failure);
        }
    }

    @Override
    public List<CampusService> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM campus_services ORDER BY name");
             ResultSet results = statement.executeQuery()) {
            List<CampusService> services = new ArrayList<>();
            while (results.next()) {
                services.add(read(results));
            }
            return List.copyOf(services);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list campus services", failure);
        }
    }

    @Override
    public void save(UUID id, CampusService service) {
        if (!Objects.requireNonNull(id, "id").equals(service.serviceId())) {
            throw new IllegalArgumentException("Repository key must match serviceId");
        }
        String updateSql = "UPDATE campus_services SET name = ?, description = ?, latitude = ?, "
                + "longitude = ? WHERE service_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            bind(update, service, id);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO campus_services (name, description, latitude, longitude, "
                                + "service_id) VALUES (?, ?, ?, ?, ?)")) {
                    bind(insert, service, id);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save campus service", failure);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM campus_services WHERE service_id = ?")) {
            statement.setString(1, id.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete campus service", failure);
        }
    }

    private static void bind(PreparedStatement statement, CampusService service, UUID id)
            throws SQLException {
        statement.setString(1, service.name());
        statement.setString(2, service.description());
        statement.setDouble(3, service.latitude());
        statement.setDouble(4, service.longitude());
        statement.setString(5, id.toString());
    }

    private static CampusService read(ResultSet result) throws SQLException {
        return new CampusService(UUID.fromString(result.getString("service_id")),
                result.getString("name"), result.getString("description"),
                result.getDouble("latitude"), result.getDouble("longitude"));
    }
}
