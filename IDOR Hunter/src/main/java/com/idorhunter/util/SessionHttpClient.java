package com.idorhunter.util;

import org.springframework.http.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deliberately simple cookie jar around {@link RestTemplate} so the
 * penetration test suite can authenticate exactly like a real browser
 * (session cookie + CSRF double-submit cookie) without pulling in an extra
 * HTTP client dependency.
 */
public class SessionHttpClient {

    private static final Pattern COOKIE_PAIR = Pattern.compile("^([^=]+)=([^;]*)");

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    public SessionHttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ResponseEntity<String> get(String path) {
        return exchange(path, HttpMethod.GET, null);
    }

    public ResponseEntity<String> post(String path, String jsonBody) {
        return exchange(path, HttpMethod.POST, jsonBody);
    }

    /** Primes the CSRF cookie, exactly like a browser loading the app for the first time. */
    public void primeCsrfCookie() {
        get("/api/auth/status");
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!cookies.isEmpty()) {
            headers.set(HttpHeaders.COOKIE, buildCookieHeader());
        }
        if (method != HttpMethod.GET && cookies.containsKey("XSRF-TOKEN")) {
            headers.set("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl + path, method, entity, String.class);
            captureCookies(response);
            return response;
        } catch (RestClientResponseException e) {
            captureCookiesFromHeaders(e.getResponseHeaders());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private void captureCookies(ResponseEntity<String> response) {
        captureCookiesFromHeaders(response.getHeaders());
    }

    private void captureCookiesFromHeaders(HttpHeaders headers) {
        if (headers == null) {
            return;
        }
        List<String> setCookies = headers.get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return;
        }
        for (String setCookie : setCookies) {
            Matcher matcher = COOKIE_PAIR.matcher(setCookie);
            if (matcher.find()) {
                cookies.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }
    }

    private String buildCookieHeader() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }
}
