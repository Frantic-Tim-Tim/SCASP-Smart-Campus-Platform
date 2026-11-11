package edu.nipa.scasp.domain;

import java.util.List;
import java.util.Optional;

public interface Repository<ID, T> {
    Optional<T> findById(ID id);

    List<T> findAll();

    void save(ID id, T entity);

    boolean deleteById(ID id);
}
