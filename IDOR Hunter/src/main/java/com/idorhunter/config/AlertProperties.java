package com.idorhunter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code idorhunter.alerts.*}. The retry schedule defaults to six
 * retry intervals, ten seconds apart, with at most three consecutive call
 * attempts per notification cycle, as required by the lab specification.
 */
@ConfigurationProperties(prefix = "idorhunter.alerts")
public class AlertProperties {

    private String provider = "mock";
    private String adminPhoneNumber = "+10000000000";
    private int retryIntervalSeconds = 10;
    private int maxAttemptsPerCycle = 3;
    private int maxRetryIntervals = 6;
    private Twilio twilio = new Twilio();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAdminPhoneNumber() {
        return adminPhoneNumber;
    }

    public void setAdminPhoneNumber(String adminPhoneNumber) {
        this.adminPhoneNumber = adminPhoneNumber;
    }

    public int getRetryIntervalSeconds() {
        return retryIntervalSeconds;
    }

    public void setRetryIntervalSeconds(int retryIntervalSeconds) {
        this.retryIntervalSeconds = retryIntervalSeconds;
    }

    public int getMaxAttemptsPerCycle() {
        return maxAttemptsPerCycle;
    }

    public void setMaxAttemptsPerCycle(int maxAttemptsPerCycle) {
        this.maxAttemptsPerCycle = maxAttemptsPerCycle;
    }

    public int getMaxRetryIntervals() {
        return maxRetryIntervals;
    }

    public void setMaxRetryIntervals(int maxRetryIntervals) {
        this.maxRetryIntervals = maxRetryIntervals;
    }

    public Twilio getTwilio() {
        return twilio;
    }

    public void setTwilio(Twilio twilio) {
        this.twilio = twilio;
    }

    public static class Twilio {
        private String accountSid = "";
        private String authToken = "";
        private String fromNumber = "";

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }
    }
}
