package com.idorhunter.repository;

import com.idorhunter.model.SecurityIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, Long> {
    List<SecurityIncident> findAllByOrderByDetectedAtDesc();
}
