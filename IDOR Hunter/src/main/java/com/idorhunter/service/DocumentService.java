package com.idorhunter.service;

import com.idorhunter.config.LabModeHolder;
import com.idorhunter.model.Document;
import com.idorhunter.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns the document read path that demonstrates the IDOR vulnerability.
 *
 * <p>In <b>secure mode</b> every direct object reference is checked against
 * the authenticated principal's identity server-side before any data is
 * returned - exactly the control that OWASP recommends for broken access
 * control (A01:2021 / API1:2023).</p>
 *
 * <p>In <b>vulnerable mode</b> that ownership check is intentionally
 * skipped, so any authenticated user can read any other user's document by
 * guessing or enumerating its numeric id. This mode exists only to let the
 * penetration tests and the UI demonstrate the flaw against seeded,
 * synthetic data; it is never the default and can only be enabled when
 * {@code idorhunter.lab.allow-mode-switch=true}.</p>
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final LabModeHolder labModeHolder;
    private final IncidentService incidentService;

    public DocumentService(DocumentRepository documentRepository, LabModeHolder labModeHolder,
                            IncidentService incidentService) {
        this.documentRepository = documentRepository;
        this.labModeHolder = labModeHolder;
        this.incidentService = incidentService;
    }

    public List<Document> listOwnedBy(String username) {
        return documentRepository.findByOwnerUsername(username);
    }

    /**
     * Fetches a single document by its direct object reference (primary key).
     *
     * @param id            the requested document id, taken directly from the URL path
     * @param requestingUser the authenticated username making the request
     * @param isAdmin        whether the requester holds the ADMIN role (admins may always read, by design)
     */
    public Document getDocumentForUser(Long id, String requestingUser, boolean isAdmin) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        boolean isOwner = document.getOwnerUsername().equals(requestingUser);
        if (isOwner || isAdmin) {
            return document;
        }

        // Cross-user access attempt: the requester does not own this resource.
        String mode = labModeHolder.isVulnerable() ? "vulnerable" : "secure";

        if (labModeHolder.isVulnerable()) {
            // VULNERABILITY: no server-side ownership check -> data is returned anyway.
            incidentService.recordCrossUserAccess(requestingUser, document.getOwnerUsername(), "Document",
                    String.valueOf(id), true, mode);
            return document;
        }

        incidentService.recordCrossUserAccess(requestingUser, document.getOwnerUsername(), "Document",
                String.valueOf(id), false, mode);
        throw new DocumentAccessDeniedException(
                "User '" + requestingUser + "' is not authorised to access document " + id);
    }
}
