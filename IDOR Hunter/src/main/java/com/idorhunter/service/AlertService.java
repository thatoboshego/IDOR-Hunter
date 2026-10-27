package com.idorhunter.service;

import com.idorhunter.model.AlertStatus;
import com.idorhunter.model.IncidentAlert;
import com.idorhunter.model.SecurityIncident;
import com.idorhunter.repository.IncidentAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Creates administrator notification cycles for confirmed security
 * incidents and hands them to {@link AlertRetryScheduler}. Guards against
 * duplicate alert storms: a new alert is only created if the incident does
 * not already have an alert cycle in progress.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final IncidentAlertRepository alertRepository;
    private final AlertRetryScheduler retryScheduler;

    public AlertService(IncidentAlertRepository alertRepository, AlertRetryScheduler retryScheduler) {
        this.alertRepository = alertRepository;
        this.retryScheduler = retryScheduler;
    }

    public synchronized IncidentAlert triggerAlert(SecurityIncident incident) {
        Optional<IncidentAlert> existing = alertRepository.findFirstByIncidentIdAndStatus(
                incident.getId(), AlertStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            log.info("Alert already in progress for incident #{}, skipping duplicate alert.", incident.getId());
            return existing.get();
        }

        IncidentAlert alert = alertRepository.save(new IncidentAlert(incident.getId()));
        retryScheduler.startCycle(alert.getId(), incident);
        return alert;
    }

    public synchronized boolean acknowledge(Long alertId) {
        Optional<IncidentAlert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty() || alertOpt.get().isAcknowledged()) {
            return false;
        }
        IncidentAlert alert = alertOpt.get();
        alert.acknowledge();
        alertRepository.save(alert);
        retryScheduler.cancel(alertId);
        return true;
    }

    public List<IncidentAlert> listAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }
}
