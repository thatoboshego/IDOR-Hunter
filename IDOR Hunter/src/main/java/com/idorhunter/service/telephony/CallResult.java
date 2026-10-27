package com.idorhunter.service.telephony;

/** Outcome of a single outbound call placement attempt. */
public class CallResult {
    private final boolean success;
    private final String providerReference;
    private final String errorMessage;

    private CallResult(boolean success, String providerReference, String errorMessage) {
        this.success = success;
        this.providerReference = providerReference;
        this.errorMessage = errorMessage;
    }

    public static CallResult success(String providerReference) {
        return new CallResult(true, providerReference, null);
    }

    public static CallResult failure(String errorMessage) {
        return new CallResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
