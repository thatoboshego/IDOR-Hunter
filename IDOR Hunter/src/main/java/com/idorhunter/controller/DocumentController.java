package com.idorhunter.controller;

import com.idorhunter.dto.DocumentResponse;
import com.idorhunter.model.Document;
import com.idorhunter.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Document management API.
 *
 * GET /api/documents/{id} is the endpoint used throughout this lab to
 * demonstrate IDOR: the id is a raw, sequential, guessable primary key, and
 * whether cross-owner requests are rejected depends entirely on the current
 * lab mode (see {@link com.idorhunter.service.DocumentService}).
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> myDocuments(Authentication authentication) {
        return documentService.listOwnedBy(authentication.getName())
                .stream()
                .map(DocumentResponse::new)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        Document document = documentService.getDocumentForUser(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(new DocumentResponse(document));
    }
}
