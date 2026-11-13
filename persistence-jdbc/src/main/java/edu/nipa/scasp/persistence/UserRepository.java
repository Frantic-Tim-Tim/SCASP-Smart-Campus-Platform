package edu.nipa.scasp.persistence;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import edu.nipa.scasp.domain.User;

public interface UserRepository {

    void save(User user) throws SQLException;

    Optional<User> findById(String userId) throws SQLException;

    List<User> findAll() throws SQLException;

    boolean update(User user) throws SQLException;

    boolean deleteById(String userId) throws SQLException;
}