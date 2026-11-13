package edu.nipa.scasp.enterprise.repository;

import java.util.List;
import java.util.Optional;

import edu.nipa.scasp.domain.User;

public interface UserRepository {

    Optional<User> findById(String userId);

    List<User> findAll();

    boolean existsById(String userId);

    User save(User user);
}