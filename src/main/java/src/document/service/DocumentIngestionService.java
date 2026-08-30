package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import src.document.dto.DocumentResponse;
import src.entity.User;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentService documentService;

    private final DocumentProcessingService
            documentProcessingService;

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

        return documentService.getDocument(
                createdDocument.id(),
                currentUser
        );
    }
}