package com.idorhunter.controller;

import com.idorhunter.config.LabModeHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Exposes the current lab mode to any authenticated user. Switching the mode is admin-only (see AdminController). */
@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabModeHolder labModeHolder;

    public LabController(LabModeHolder labModeHolder) {
        this.labModeHolder = labModeHolder;
    }

    @GetMapping("/mode")
    public Map<String, Object> getMode() {
        return Map.of(
                "mode", labModeHolder.getMode().name(),
                "switchingAllowed", labModeHolder.isSwitchingAllowed());
    }
}
