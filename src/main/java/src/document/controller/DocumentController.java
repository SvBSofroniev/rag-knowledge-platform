package src.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import src.document.dto.DocumentResponse;
import src.document.service.DocumentService;
import src.entity.User;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/workspaces/{workspaceId}/documents")
    public DocumentResponse uploadDocument(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService.uploadDocument(workspaceId, file, currentUser);
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    public List<DocumentResponse> getWorkspaceDocuments(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService.getWorkspaceDocuments(workspaceId, currentUser);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentResponse getDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService.getDocument(documentId, currentUser);
    }

    @DeleteMapping("/documents/{documentId}")
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        documentService.deleteDocument(documentId, currentUser);
    }
}
