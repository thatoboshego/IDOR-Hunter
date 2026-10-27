package com.idorhunter.service;

import com.idorhunter.model.*;
import com.idorhunter.repository.IncidentAlertRepository;
import com.idorhunter.repository.SecurityIncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies the alert retry schedule using the default mock telephony
 * provider: no real call is ever placed. Uses a short, test-only interval
 * so the suite runs quickly and deterministically.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "idorhunter.alerts.provider=mock",
        "idorhunter.alerts.retry-interval-seconds=1",
        "idorhunter.alerts.max-attempts-per-cycle=2",
        "idorhunter.alerts.max-retry-intervals=2"
})
class AlertRetrySchedulerTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private IncidentAlertRepository incidentAlertRepository;

    @Autowired
    private SecurityIncidentRepository incidentRepository;

    private SecurityIncident newIncident() {
        SecurityIncident incident = new SecurityIncident("usera", "userb", "Document", "99",
                Severity.CRITICAL, IncidentOutcome.CONFIRMED_BREACH, "vulnerable");
        return incidentRepository.save(incident);
    }

    @Test
    void retryLoop_stopsAtMaxAttemptsAndMarksExhausted() {
        SecurityIncident incident = newIncident();

        alertService.triggerAlert(incident);

        await().atMost(6, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<IncidentAlert> alert = incidentAlertRepository
                    .findFirstByIncidentIdAndStatus(incident.getId(), AlertStatus.EXHAUSTED);
            assertThat(alert).isPresent();
            assertThat(alert.get().getAttemptsMade()).isEqualTo(2);
        });
    }

    @Test
    void duplicateTrigger_doesNotStartASecondConcurrentAlert() {
        SecurityIncident incident = newIncident();

        IncidentAlert first = alertService.triggerAlert(incident);
        IncidentAlert second = alertService.triggerAlert(incident);

        assertThat(second.getId()).isEqualTo(first.getId());
        List<IncidentAlert> inProgress = incidentAlertRepository
                .findByIncidentIdAndStatus(incident.getId(), AlertStatus.IN_PROGRESS);
        assertThat(inProgress.size()).isLessThanOrEqualTo(1);
    }

    @Test
    void acknowledgedAlert_stopsFurtherAttempts() throws InterruptedException {
        SecurityIncident incident = newIncident();
        IncidentAlert alert = alertService.triggerAlert(incident);

        TimeUnit.MILLISECONDS.sleep(200);
        alertService.acknowledge(alert.getId());

        int attemptsAtAck = incidentAlertRepository.findById(alert.getId()).orElseThrow().getAttemptsMade();
        TimeUnit.SECONDS.sleep(3);
        int attemptsAfterWaiting = incidentAlertRepository.findById(alert.getId()).orElseThrow().getAttemptsMade();

        assertThat(attemptsAfterWaiting).isEqualTo(attemptsAtAck);
    }
}
