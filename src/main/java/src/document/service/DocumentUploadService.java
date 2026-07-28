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
public class DocumentUploadService {

    private final DocumentService documentService;
    private final DocumentProcessingService processingService;
    private final DocumentRepository documentRepository;

    public DocumentResponse uploadAndProcess(
            UUID workspaceId,
            MultipartFile file,
            User currentUser
    ) {
        DocumentResponse created = documentService.uploadDocument(
                workspaceId,
                file,
                currentUser
        );

        processingService.processDocument(created.id());

        Document updated = documentRepository.findById(created.id())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found")
                );

        return documentService.toResponse(updated);
    }
}