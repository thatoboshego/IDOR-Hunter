package com.idorhunter.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks a single administrator notification cycle for one {@link SecurityIncident}.
 * At most one active (non-terminal) alert may exist per incident at a time,
 * which is enforced by {@code IncidentAlertRepository} + {@code AlertService}.
 */
@Entity
@Table(name = "incident_alert")
public class IncidentAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private int attemptsMade;

    @Column(nullable = false)
    private boolean acknowledged;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.IN_PROGRESS;

    @Column(length = 2000)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant lastAttemptAt;
    private Instant acknowledgedAt;

    protected IncidentAlert() {
    }

    public IncidentAlert(Long incidentId) {
        this.incidentId = incidentId;
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public int getAttemptsMade() {
        return attemptsMade;
    }

    public void recordAttempt(Instant when, String error) {
        this.attemptsMade++;
        this.lastAttemptAt = when;
        this.lastError = error;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void acknowledge() {
        this.acknowledged = true;
        this.acknowledgedAt = Instant.now();
        this.status = AlertStatus.ACKNOWLEDGED;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
