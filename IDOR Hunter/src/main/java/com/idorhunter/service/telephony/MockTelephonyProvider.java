package com.idorhunter.service.telephony;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default telephony provider for the lab and for every automated test.
 * It never performs network I/O; it only records that a call "would have
 * been placed" so the retry schedule, logging and dashboard can be
 * exercised safely in CI and during ordinary development.
 */
@Component
@ConditionalOnProperty(prefix = "idorhunter.alerts", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockTelephonyProvider implements TelephonyProvider {

    private static final Logger log = LoggerFactory.getLogger(MockTelephonyProvider.class);
    private final AtomicInteger callCounter = new AtomicInteger();

    @Override
    public CallResult placeCall(String toPhoneNumber, String message) {
        int callId = callCounter.incrementAndGet();
        log.info("[MOCK CALL #{}] would dial {} with message: \"{}\"", callId, toPhoneNumber, message);
        return CallResult.success("mock-call-" + callId);
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
