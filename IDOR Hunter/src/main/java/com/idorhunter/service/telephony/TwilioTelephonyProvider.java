package com.idorhunter.service.telephony;

import com.idorhunter.config.AlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Places a real outbound phone call through the Twilio Programmable Voice
 * REST API (https://www.twilio.com/docs/voice/api/call-resource). Only
 * instantiated when {@code idorhunter.alerts.provider=twilio}; credentials
 * are read exclusively from environment variables (see .env.example) and
 * are never logged.
 */
@Component
@ConditionalOnProperty(prefix = "idorhunter.alerts", name = "provider", havingValue = "twilio")
public class TwilioTelephonyProvider implements TelephonyProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioTelephonyProvider.class);
    private static final String CALLS_URL = "https://api.twilio.com/2010-04-01/Accounts/{sid}/Calls.json";

    private final RestTemplate restTemplate = new RestTemplate();
    private final AlertProperties properties;

    public TwilioTelephonyProvider(AlertProperties properties) {
        this.properties = properties;
        if (properties.getTwilio().getAccountSid().isBlank() || properties.getTwilio().getAuthToken().isBlank()) {
            log.warn("Twilio provider selected but TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN are not set. "
                    + "Calls will fail until real credentials are supplied via environment variables.");
        }
    }

    @Override
    public CallResult placeCall(String toPhoneNumber, String message) {
        AlertProperties.Twilio twilio = properties.getTwilio();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilio.getAccountSid(), twilio.getAuthToken());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toPhoneNumber);
        form.add("From", twilio.getFromNumber());
        form.add("Twiml", "<Response><Say>" + escapeXml(message) + "</Say></Response>");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    CALLS_URL, HttpMethod.POST, request, String.class, twilio.getAccountSid());
            log.info("Twilio call placed to {}, status={}", toPhoneNumber, response.getStatusCode());
            return CallResult.success(response.getStatusCode().toString());
        } catch (RestClientException e) {
            log.error("Twilio call to {} failed: {}", toPhoneNumber, e.getMessage());
            return CallResult.failure(e.getMessage());
        }
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public String providerName() {
        return "twilio";
    }
}
