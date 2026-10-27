package com.idorhunter.repository;

import com.idorhunter.model.AlertStatus;
import com.idorhunter.model.IncidentAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IncidentAlertRepository extends JpaRepository<IncidentAlert, Long> {
    List<IncidentAlert> findByIncidentIdAndStatus(Long incidentId, AlertStatus status);
    Optional<IncidentAlert> findFirstByIncidentIdAndStatus(Long incidentId, AlertStatus status);
    List<IncidentAlert> findAllByOrderByCreatedAtDesc();
}
