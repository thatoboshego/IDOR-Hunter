package com.idorhunter.controller;

import com.idorhunter.config.LabModeHolder;
import com.idorhunter.model.AppUser;
import com.idorhunter.model.Document;
import com.idorhunter.model.IncidentAlert;
import com.idorhunter.model.SecurityIncident;
import com.idorhunter.repository.DocumentRepository;
import com.idorhunter.repository.UserRepository;
import com.idorhunter.service.AlertService;
import com.idorhunter.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Administrator-only operations: switching lab mode, reviewing incidents, and acknowledging alerts. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LabModeHolder labModeHolder;
    private final IncidentService incidentService;
    private final AlertService alertService;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public AdminController(LabModeHolder labModeHolder, IncidentService incidentService, AlertService alertService,
                            UserRepository userRepository, DocumentRepository documentRepository) {
        this.labModeHolder = labModeHolder;
        this.incidentService = incidentService;
        this.alertService = alertService;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/users")
    public List<Map<String, String>> users() {
        return userRepository.findAll().stream()
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "displayName", u.getDisplayName(),
                        "role", u.getRole().name()))
                .toList();
    }

    @GetMapping("/documents")
    public List<Document> allDocuments() {
        return documentRepository.findAll();
    }

    @PostMapping("/lab/mode")
    public ResponseEntity<?> setMode(@RequestBody Map<String, String> body) {
        String requested = body.getOrDefault("mode", "");
        LabModeHolder.Mode target;
        try {
            target = LabModeHolder.Mode.valueOf(requested.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "mode must be SECURE or VULNERABLE"));
        }

        boolean switched = labModeHolder.switchTo(target);
        if (!switched) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "MODE_SWITCH_DISABLED",
                    "message", "Mode switching is disabled by configuration (idorhunter.lab.allow-mode-switch)"));
        }
        return ResponseEntity.ok(Map.of("mode", labModeHolder.getMode().name()));
    }

    @GetMapping("/incidents")
    public List<SecurityIncident> incidents() {
        return incidentService.listAll();
    }

    @GetMapping("/alerts")
    public List<IncidentAlert> alerts() {
        return alertService.listAll();
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<?> acknowledge(@PathVariable Long id) {
        boolean acknowledged = alertService.acknowledge(id);
        if (!acknowledged) {
            return ResponseEntity.status(409).body(Map.of("error", "Alert already acknowledged or not found"));
        }
        return ResponseEntity.ok(Map.of("acknowledged", true));
    }
}
