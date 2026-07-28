package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import src.common.exception.ResourceNotFoundException;
import src.document.dto.DocumentResponse;
import src.document.repository.DocumentRepository;
import src.entity.Document;
import src.entity.User;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentRepository documentRepository;

    public DocumentResponse ingest(
            UUID workspaceId,
            MultipartFile file,
            User currentUser
    ) {
        DocumentResponse createdDocument =
                documentService.uploadDocument(
                        workspaceId,
                        file,
                        currentUser
                );

        documentProcessingService.processDocument(
                createdDocument.id()
        );

        Document processedDocument = documentRepository
                .findById(createdDocument.id())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        return documentService.toResponse(processedDocument);
    }
}