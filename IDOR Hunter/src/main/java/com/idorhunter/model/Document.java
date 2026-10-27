package com.idorhunter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** A synthetic document owned by exactly one lab user. */
@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classification classification;

    @NotBlank
    @Column(nullable = false)
    private String ownerUsername;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Document() {
    }

    public Document(String title, String content, Classification classification, String ownerUsername) {
        this.title = title;
        this.content = content;
        this.classification = classification;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Classification getClassification() {
        return classification;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
