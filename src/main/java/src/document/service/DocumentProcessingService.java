package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.Document;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentTextExtractor textExtractor;
    private final TextChunker textChunker;
    private final DocumentChunkPersistenceService chunkPersistenceService;
    private final DocumentStatusService statusService;

    public void processDocument(UUID documentId) {
        Document document = getDocument(documentId);

        validateStatus(document);

        statusService.markProcessing(documentId);

        try {
            /*
             * Reload after the independent status transaction.
             */
            document = getDocument(documentId);

            String extractedText = textExtractor.extract(document);

            List<String> chunks = textChunker.chunk(extractedText);

            if (chunks.isEmpty()) {
                throw new RuntimeException(
                        "Document produced no usable text chunks"
                );
            }

            chunkPersistenceService.replaceChunks(
                    documentId,
                    chunks
            );

            statusService.markReady(documentId);

        } catch (Exception exception) {
            statusService.markFailed(
                    documentId,
                    getErrorMessage(exception)
            );

            /*
             * Do not rethrow for the initial synchronous MVP.
             * The upload request can return a document with FAILED status.
             */
        }
    }

    private Document getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );
    }

    private void validateStatus(Document document) {
        if (document.getStatus() == DocumentStatus.PROCESSING) {
            throw new RuntimeException(
                    "Document is already being processed"
            );
        }

        if (document.getStatus() == DocumentStatus.READY) {
            throw new RuntimeException(
                    "Document has already been processed"
            );
        }
    }

    private String getErrorMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}