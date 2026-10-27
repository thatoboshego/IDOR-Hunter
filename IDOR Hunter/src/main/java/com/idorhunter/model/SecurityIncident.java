package com.idorhunter.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A recorded broken-access-control event: someone authenticated as one user
 * requested a resource owned by a different user. Created regardless of lab
 * mode so the detection layer is exercised even when enforcement is on.
 */
@Entity
@Table(name = "security_incident")
public class SecurityIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requestingUser;

    @Column(nullable = false)
    private String resourceOwner;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentOutcome outcome;

    @Column(nullable = false)
    private String labMode;

    @Column(nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(nullable = false)
    private boolean remediated;

    @Column(length = 1000)
    private String remediationNotes;

    protected SecurityIncident() {
    }

    public SecurityIncident(String requestingUser, String resourceOwner, String resourceType, String resourceId,
                             Severity severity, IncidentOutcome outcome, String labMode) {
        this.requestingUser = requestingUser;
        this.resourceOwner = resourceOwner;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.severity = severity;
        this.outcome = outcome;
        this.labMode = labMode;
        this.remediated = outcome == IncidentOutcome.BLOCKED_ATTEMPT;
        this.remediationNotes = outcome == IncidentOutcome.BLOCKED_ATTEMPT
                ? "Blocked automatically by server-side ownership check."
                : "Enforce server-side ownership checks on every direct object reference; switch the lab to secure mode.";
    }

    public Long getId() {
        return id;
    }

    public String getRequestingUser() {
        return requestingUser;
    }

    public String getResourceOwner() {
        return resourceOwner;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public IncidentOutcome getOutcome() {
        return outcome;
    }

    public String getLabMode() {
        return labMode;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public boolean isRemediated() {
        return remediated;
    }

    public String getRemediationNotes() {
        return remediationNotes;
    }
}
