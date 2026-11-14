package edu.nipa.scasp.enterprise.service;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class DuplicateCourseException extends RuntimeException {

    public DuplicateCourseException(String message) {
        super(message);
    }
}
