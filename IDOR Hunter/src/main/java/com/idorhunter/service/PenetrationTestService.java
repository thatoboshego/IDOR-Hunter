package com.idorhunter.service;

import com.idorhunter.config.LabModeHolder;
import com.idorhunter.model.Document;
import com.idorhunter.model.TestResult;
import com.idorhunter.repository.DocumentRepository;
import com.idorhunter.repository.TestResultRepository;
import com.idorhunter.util.LabCredentials;
import com.idorhunter.util.ServerPortCapture;
import com.idorhunter.util.SessionHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the same IDOR penetration tests the UI's "Security Lab" tab and the
 * JUnit integration test suite both rely on, but over real HTTP against the
 * running application - exactly as an external tester would. All targets
 * are local, seeded, synthetic documents (see {@link com.idorhunter.util.DataSeeder}).
 */
@Service
public class PenetrationTestService {

    private static final Logger log = LoggerFactory.getLogger(PenetrationTestService.class);

    private final ServerPortCapture portCapture;
    private final DocumentRepository documentRepository;
    private final TestResultRepository testResultRepository;
    private final LabModeHolder labModeHolder;

    public PenetrationTestService(ServerPortCapture portCapture, DocumentRepository documentRepository,
                                   TestResultRepository testResultRepository, LabModeHolder labModeHolder) {
        this.portCapture = portCapture;
        this.documentRepository = documentRepository;
        this.testResultRepository = testResultRepository;
        this.labModeHolder = labModeHolder;
    }

    public List<TestResult> runFullSuite() {
        int port = portCapture.getPort();
        if (port <= 0) {
            throw new IllegalStateException("Server port not yet known; the application is still starting.");
        }
        String baseUrl = "http://localhost:" + port;
        String mode = labModeHolder.isVulnerable() ? "vulnerable" : "secure";

        Long ownDocId = requireFirstDocId(LabCredentials.USER_A_USERNAME);
        Long otherUserDocId = requireFirstDocId(LabCredentials.USER_B_USERNAME);

        List<TestResult> results = new ArrayList<>();
        results.add(testOwnResourceAccess(baseUrl, mode, ownDocId));
        results.add(testCrossUserAccess(baseUrl, mode, otherUserDocId));
        results.add(testNonexistentResource(baseUrl, mode));
        results.add(testUnauthenticatedRequest(baseUrl, mode, ownDocId));

        testResultRepository.saveAll(results);
        return results;
    }

    private Long requireFirstDocId(String username) {
        return documentRepository.findByOwnerUsername(username).stream()
                .findFirst()
                .map(Document::getId)
                .orElseThrow(() -> new IllegalStateException("No seeded document found for " + username));
    }

    private TestResult testOwnResourceAccess(String baseUrl, String mode, Long ownDocId) {
        SessionHttpClient client = loggedInAs(baseUrl, LabCredentials.USER_A_USERNAME, LabCredentials.USER_A_PASSWORD);
        ResponseEntity<String> response = client.get("/api/documents/" + ownDocId);

        boolean passed = response.getStatusCode() == HttpStatus.OK
                && bodyContainsOwner(response.getBody(), LabCredentials.USER_A_USERNAME);

        return new TestResult("Own resource access", mode, "GET /api/documents/" + ownDocId,
                "200 OK with the requester's own document",
                statusAndSnippet(response), passed, evidence("usera", response));
    }

    private TestResult testCrossUserAccess(String baseUrl, String mode, Long otherUserDocId) {
        SessionHttpClient client = loggedInAs(baseUrl, LabCredentials.USER_A_USERNAME, LabCredentials.USER_A_PASSWORD);
        ResponseEntity<String> response = client.get("/api/documents/" + otherUserDocId);

        boolean dataExposed = response.getStatusCode() == HttpStatus.OK
                && bodyContainsOwner(response.getBody(), LabCredentials.USER_B_USERNAME);
        boolean correctlyBlocked = response.getStatusCode() == HttpStatus.FORBIDDEN;

        // "Passed" means the API behaved securely: a genuine data exposure is a FAIL regardless
        // of the HTTP status code looking superficially fine.
        boolean passed = correctlyBlocked && !dataExposed;

        String actual = dataExposed
                ? "200 OK - User B's private document content was returned to User A (IDOR confirmed)"
                : statusAndSnippet(response);

        return new TestResult("Unauthorised cross-user access (IDOR)", mode, "GET /api/documents/" + otherUserDocId,
                "403 Forbidden - cross-owner access must be rejected",
                actual, passed, evidence("userb", response));
    }

    private TestResult testNonexistentResource(String baseUrl, String mode) {
        SessionHttpClient client = loggedInAs(baseUrl, LabCredentials.USER_A_USERNAME, LabCredentials.USER_A_PASSWORD);
        ResponseEntity<String> response = client.get("/api/documents/999999");
        boolean passed = response.getStatusCode() == HttpStatus.NOT_FOUND;

        return new TestResult("Nonexistent resource", mode, "GET /api/documents/999999",
                "404 Not Found", statusAndSnippet(response), passed, evidence(null, response));
    }

    private TestResult testUnauthenticatedRequest(String baseUrl, String mode, Long ownDocId) {
        SessionHttpClient anonymous = new SessionHttpClient(baseUrl);
        ResponseEntity<String> response = anonymous.get("/api/documents/" + ownDocId);
        boolean passed = response.getStatusCode() == HttpStatus.UNAUTHORIZED;

        return new TestResult("Unauthenticated request", mode, "GET /api/documents/" + ownDocId,
                "401 Unauthorized", statusAndSnippet(response), passed, evidence(null, response));
    }

    private SessionHttpClient loggedInAs(String baseUrl, String username, String password) {
        SessionHttpClient client = new SessionHttpClient(baseUrl);
        client.primeCsrfCookie();
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<String> loginResponse = client.post("/api/auth/login", body);
        if (loginResponse.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Pentest login failed for " + username + ": " + loginResponse.getStatusCode());
        }
        return client;
    }

    private boolean bodyContainsOwner(String body, String owner) {
        return body != null && body.contains("\"ownerUsername\":\"" + owner + "\"");
    }

    private String statusAndSnippet(ResponseEntity<String> response) {
        return response.getStatusCode() + " " + truncate(response.getBody());
    }

    private String evidence(String expectedOwnerHint, ResponseEntity<String> response) {
        String snippet = truncate(response.getBody());
        return "HTTP " + response.getStatusCode().value() + " | response: " + snippet;
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    public List<TestResult> history() {
        return testResultRepository.findAllByOrderByExecutedAtDesc();
    }
}
