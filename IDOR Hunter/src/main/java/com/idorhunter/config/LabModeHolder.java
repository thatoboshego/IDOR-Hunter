package com.idorhunter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the lab's current access-control mode at runtime.
 *
 * The application always starts in the mode configured by
 * {@code idorhunter.lab.mode} (secure by default). Switching is only
 * permitted when {@code idorhunter.lab.allow-mode-switch} is true, which
 * keeps a hardened/production configuration from ever being weakened by an
 * API call. This class never hard-codes "vulnerable" as a default.
 */
@Component
public class LabModeHolder {

    public enum Mode { SECURE, VULNERABLE }

    private final AtomicReference<Mode> mode;
    private final boolean switchingAllowed;

    public LabModeHolder(@Value("${idorhunter.lab.mode:secure}") String initialMode,
                          @Value("${idorhunter.lab.allow-mode-switch:false}") boolean switchingAllowed) {
        this.mode = new AtomicReference<>(parse(initialMode));
        this.switchingAllowed = switchingAllowed;
    }

    private Mode parse(String raw) {
        return "vulnerable".equalsIgnoreCase(raw) ? Mode.VULNERABLE : Mode.SECURE;
    }

    public Mode getMode() {
        return mode.get();
    }

    public boolean isVulnerable() {
        return mode.get() == Mode.VULNERABLE;
    }

    public boolean isSwitchingAllowed() {
        return switchingAllowed;
    }

    /** @return true if the mode was changed. */
    public boolean switchTo(Mode target) {
        if (!switchingAllowed) {
            return false;
        }
        mode.set(target);
        return true;
    }
}
