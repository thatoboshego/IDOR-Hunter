package com.idorhunter.util;

/**
 * Deterministic, synthetic lab credentials. These accounts only ever exist
 * in the in-memory H2 database seeded at startup and are documented here
 * purely so the lab is reproducible - they are not real secrets.
 */
public final class LabCredentials {

    public static final String USER_A_USERNAME = "usera";
    public static final String USER_B_USERNAME = "userb";
    public static final String ADMIN_USERNAME = "admin";

    public static final String USER_A_PASSWORD = "UserA-Lab-Pass1!";
    public static final String USER_B_PASSWORD = "UserB-Lab-Pass1!";
    public static final String ADMIN_PASSWORD = "Admin-Lab-Pass1!";

    private LabCredentials() {
    }
}
