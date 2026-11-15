package edu.nipa.scasp.enterprise.service;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}