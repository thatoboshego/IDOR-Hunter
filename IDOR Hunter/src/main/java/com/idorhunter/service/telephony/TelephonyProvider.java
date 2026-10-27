package com.idorhunter.service.telephony;

/** Abstraction over an outbound phone-call provider used for administrator incident alerts. */
public interface TelephonyProvider {

    CallResult placeCall(String toPhoneNumber, String message);

    /** Short identifier used in logs and test-result evidence, e.g. "mock" or "twilio". */
    String providerName();
}
