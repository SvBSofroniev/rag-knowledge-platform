package src.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import src.chat.dto.ChatSessionResponse;
import src.document.dto.DocumentResponse;
import src.document.dto.GlobalDocumentResponse;
import src.document.service.DocumentChatService;
import src.document.service.DocumentIngestionService;
import src.document.service.DocumentService;
import src.entity.User;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import src.document.dto.DocumentContentResponse;
import src.document.dto.DocumentDetailsResponse;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentIngestionService documentIngestionService;
    private final DocumentChatService documentChatService;

    @PostMapping("/workspaces/{workspaceId}/documents")
    public DocumentResponse uploadDocument(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentIngestionService.ingest(
                workspaceId,
                file,
                currentUser
        );
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    public List<DocumentResponse> getWorkspaceDocuments(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService.getWorkspaceDocuments(
                workspaceId,
                currentUser
        );
    }

    @GetMapping("/documents/{documentId}")
    public DocumentResponse getDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService.getDocument(
                documentId,
                currentUser
        );
    }

    @DeleteMapping("/documents/{documentId}")
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        documentService.deleteDocument(
                documentId,
                currentUser
        );
    }

    @GetMapping("/documents/{documentId}/details")
    public DocumentDetailsResponse getDocumentDetails(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService
                .getDocumentDetails(
                        documentId,
                        currentUser
                );
    }

    @GetMapping("/documents/{documentId}/content")
    public DocumentContentResponse getDocumentContent(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentService
                .getDocumentContent(
                        documentId,
                        currentUser
                );
    }

    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<Resource> previewDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        DocumentService.DocumentFile file =
                documentService.getDocumentFile(
                        documentId,
                        currentUser
                );

        MediaType mediaType =
                resolveMediaType(
                        file
                );

        String contentDisposition =
                ContentDisposition
                        .inline()
                        .filename(
                                file.originalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build()
                        .toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(
                        file.fileSize()
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(
                        file.resource()
                );
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        DocumentService.DocumentFile file =
                documentService.getDocumentFile(
                        documentId,
                        currentUser
                );

        MediaType mediaType =
                resolveMediaType(
                        file
                );

        String contentDisposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                file.originalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build()
                        .toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(
                        file.fileSize()
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(
                        file.resource()
                );
    }

    private MediaType resolveMediaType(
            DocumentService.DocumentFile file
    ) {
        if (file.contentType() != null &&
                !file.contentType().isBlank()) {

            try {
                return MediaType.parseMediaType(
                        file.contentType()
                );

            } catch (IllegalArgumentException ignored) {
                // Fall back to filename detection.
            }
        }

        return MediaTypeFactory
                .getMediaType(
                        file.originalFilename()
                )
                .orElse(
                        MediaType.APPLICATION_OCTET_STREAM
                );
    }

    @PostMapping("/documents/{documentId}/chat")
    public ChatSessionResponse createChatForDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return documentChatService
                .createChatForDocument(
                        documentId,
                        currentUser
                );
    }

    @GetMapping("/documents")
    public ResponseEntity<List<GlobalDocumentResponse>>
    getAccessibleDocuments(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(
                documentService.getAccessibleDocuments(
                        currentUser
                )
        );
    }
}