package com.idorhunter.service;

import com.idorhunter.model.*;
import com.idorhunter.repository.SecurityIncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Detection layer: records every cross-user resource access attempt as a
 * {@link SecurityIncident}, independent of whether the lab is in vulnerable
 * or secure mode. Confirmed breaches (vulnerable mode) escalate to the
 * {@link AlertService} to notify the administrator.
 */
@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final SecurityIncidentRepository incidentRepository;
    private final AlertService alertService;

    public IncidentService(SecurityIncidentRepository incidentRepository, AlertService alertService) {
        this.incidentRepository = incidentRepository;
        this.alertService = alertService;
    }

    public SecurityIncident recordCrossUserAccess(String requestingUser, String resourceOwner, String resourceType,
                                                    String resourceId, boolean accessSucceeded, String labMode) {
        Severity severity = accessSucceeded ? Severity.CRITICAL : Severity.LOW;
        IncidentOutcome outcome = accessSucceeded ? IncidentOutcome.CONFIRMED_BREACH : IncidentOutcome.BLOCKED_ATTEMPT;

        SecurityIncident incident = new SecurityIncident(requestingUser, resourceOwner, resourceType, resourceId,
                severity, outcome, labMode);
        incident = incidentRepository.save(incident);

        if (accessSucceeded) {
            log.warn("CONFIRMED IDOR BREACH: user '{}' read {} #{} owned by '{}' (mode={})",
                    requestingUser, resourceType, resourceId, resourceOwner, labMode);
            alertService.triggerAlert(incident);
        } else {
            log.info("Blocked cross-user access attempt: user '{}' -> {} #{} owned by '{}' (mode={})",
                    requestingUser, resourceType, resourceId, resourceOwner, labMode);
        }
        return incident;
    }

    public List<SecurityIncident> listAll() {
        return incidentRepository.findAllByOrderByDetectedAtDesc();
    }
}
