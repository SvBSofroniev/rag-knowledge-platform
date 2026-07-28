package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import src.common.exception.ResourceNotFoundException;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.Document;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStatusService {

    private final DocumentRepository documentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(UUID documentId) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.PROCESSING);
        document.setProcessingError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReady(UUID documentId) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.READY);
        document.setProcessingError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            UUID documentId,
            String errorMessage
    ) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.FAILED);
        document.setProcessingError(limitErrorMessage(errorMessage));
    }

    private Document getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found"
                        )
                );
    }

    private String limitErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown document processing error";
        }

        int maximumLength = 2_000;

        if (message.length() <= maximumLength) {
            return message;
        }

        return message.substring(0, maximumLength);
    }
}