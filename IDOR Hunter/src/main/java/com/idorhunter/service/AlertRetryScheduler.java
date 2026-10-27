package com.idorhunter.service;

import com.idorhunter.config.AlertProperties;
import com.idorhunter.model.AlertStatus;
import com.idorhunter.model.IncidentAlert;
import com.idorhunter.model.SecurityIncident;
import com.idorhunter.repository.IncidentAlertRepository;
import com.idorhunter.service.telephony.CallResult;
import com.idorhunter.service.telephony.TelephonyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs the administrator phone-call retry schedule for a single
 * {@link IncidentAlert} cycle.
 *
 * <p>Schedule: up to {@code max-attempts-per-cycle} (default 3) consecutive
 * call attempts, each {@code retry-interval-seconds} (default 10s) apart,
 * bounded overall by {@code max-retry-intervals} (default 6) scheduled
 * ticks as a safety ceiling. The loop stops immediately once the alert is
 * acknowledged. Exactly one scheduled loop can be active per alert id at a
 * time, which prevents concurrent retry loops and duplicate call storms for
 * the same incident.</p>
 */
@Component
public class AlertRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertRetryScheduler.class);

    private final TelephonyProvider telephonyProvider;
    private final IncidentAlertRepository alertRepository;
    private final AlertProperties properties;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> activeLoops = new ConcurrentHashMap<>();

    public AlertRetryScheduler(TelephonyProvider telephonyProvider, IncidentAlertRepository alertRepository,
                                AlertProperties properties) {
        this.telephonyProvider = telephonyProvider;
        this.alertRepository = alertRepository;
        this.properties = properties;
    }

    /** Starts (or re-enters, idempotently) the retry cycle for one alert. */
    public void startCycle(Long alertId, SecurityIncident incident) {
        if (activeLoops.containsKey(alertId)) {
            log.debug("Retry loop already active for alert #{}", alertId);
            return;
        }
        String message = "IDOR Hunter security alert. User " + incident.getRequestingUser()
                + " accessed a document owned by " + incident.getResourceOwner()
                + " without authorisation. Please acknowledge in the dashboard.";
        runTick(alertId, message, 1);
    }

    private void runTick(Long alertId, String message, int tickNumber) {
        ScheduledFuture<?> future = executor.schedule(() -> attempt(alertId, message, tickNumber),
                tickNumber == 1 ? 0 : properties.getRetryIntervalSeconds(), TimeUnit.SECONDS);
        activeLoops.put(alertId, future);
    }

    private void attempt(Long alertId, String message, int tickNumber) {
        IncidentAlert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null || alert.isAcknowledged()) {
            activeLoops.remove(alertId);
            return;
        }

        boolean withinAttemptBudget = tickNumber <= properties.getMaxAttemptsPerCycle();
        boolean withinIntervalCeiling = tickNumber <= properties.getMaxRetryIntervals();

        if (withinAttemptBudget && withinIntervalCeiling) {
            CallResult result = telephonyProvider.placeCall(resolveAdminPhoneNumber(), message);
            alert.recordAttempt(Instant.now(), result.isSuccess() ? null : result.getErrorMessage());
            if (!result.isSuccess()) {
                alert.setStatus(AlertStatus.FAILED);
            }
            alertRepository.save(alert);
            log.info("Alert #{} attempt {}/{} via '{}': success={}", alertId, tickNumber,
                    properties.getMaxAttemptsPerCycle(), telephonyProvider.providerName(), result.isSuccess());
        }

        boolean moreTicksRemain = tickNumber < properties.getMaxRetryIntervals()
                && tickNumber < properties.getMaxAttemptsPerCycle();

        if (!alert.isAcknowledged() && moreTicksRemain) {
            runTick(alertId, message, tickNumber + 1);
        } else {
            activeLoops.remove(alertId);
            if (!alert.isAcknowledged()) {
                alert.setStatus(AlertStatus.EXHAUSTED);
                alertRepository.save(alert);
                log.warn("Alert #{} exhausted after {} attempt(s) without acknowledgement.", alertId,
                        alert.getAttemptsMade());
            }
        }
    }

    private String resolveAdminPhoneNumber() {
        return properties.getAdminPhoneNumber();
    }

    public void cancel(Long alertId) {
        ScheduledFuture<?> future = activeLoops.remove(alertId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
