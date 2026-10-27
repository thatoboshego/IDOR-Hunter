package com.idorhunter.dto;

import com.idorhunter.model.Classification;
import com.idorhunter.model.Document;
import java.time.Instant;

public class DocumentResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final Classification classification;
    private final String ownerUsername;
    private final Instant createdAt;

    public DocumentResponse(Document d) {
        this.id = d.getId();
        this.title = d.getTitle();
        this.content = d.getContent();
        this.classification = d.getClassification();
        this.ownerUsername = d.getOwnerUsername();
        this.createdAt = d.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Classification getClassification() { return classification; }
    public String getOwnerUsername() { return ownerUsername; }
    public Instant getCreatedAt() { return createdAt; }
}
