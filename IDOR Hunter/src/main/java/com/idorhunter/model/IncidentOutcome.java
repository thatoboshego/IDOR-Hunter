package com.idorhunter.model;

/** Whether an attempted cross-user access actually succeeded against the resource. */
public enum IncidentOutcome {
    /** Vulnerable mode: the cross-user request returned the other user's data. */
    CONFIRMED_BREACH,
    /** Secure mode: server-side authorisation correctly blocked the attempt. */
    BLOCKED_ATTEMPT
}
