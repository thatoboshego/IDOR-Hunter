package com.idorhunter.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Outcome of one penetration test case run against the document API. */
@Entity
@Table(name = "test_result")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    @Column(nullable = false)
    private String labMode;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String expectedOutcome;

    @Column(nullable = false)
    private String actualOutcome;

    @Column(nullable = false)
    private boolean passed;

    @Column(length = 2000)
    private String evidence;

    @Column(nullable = false)
    private Instant executedAt = Instant.now();

    protected TestResult() {
    }

    public TestResult(String testName, String labMode, String endpoint, String expectedOutcome,
                       String actualOutcome, boolean passed, String evidence) {
        this.testName = testName;
        this.labMode = labMode;
        this.endpoint = endpoint;
        this.expectedOutcome = expectedOutcome;
        this.actualOutcome = actualOutcome;
        this.passed = passed;
        this.evidence = evidence;
    }

    public Long getId() {
        return id;
    }

    public String getTestName() {
        return testName;
    }

    public String getLabMode() {
        return labMode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getEvidence() {
        return evidence;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
