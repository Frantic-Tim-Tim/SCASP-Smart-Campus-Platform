package edu.nipa.scasp.enterprise.bean;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * Initial deployment test for the SCASP Enterprise Services module.
 *
 * A stateless session bean is managed by the EJB container.
 */
@Stateless
@LocalBean
public class HealthBean {

    public String checkHealth() {
        return "SCASP Enterprise Services is running!";
    }
}