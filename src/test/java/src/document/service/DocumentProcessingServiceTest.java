package src.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.common.exception.ConflictException;
import src.common.exception.ResourceNotFoundException;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentRepository
            documentRepository;

    @Mock
    private DocumentTextExtractor
            textExtractor;

    @Mock
    private TextChunker
            textChunker;

    @Mock
    private DocumentChunkPersistenceService
            chunkPersistenceService;

    @Mock
    private DocumentStatusService
            statusService;

    @Mock
    private Document
            document;

    private DocumentProcessingService
            documentProcessingService;

    private UUID documentId;

    @BeforeEach
    void setUp() {
        documentProcessingService =
                new DocumentProcessingService(
                        documentRepository,
                        textExtractor,
                        textChunker,
                        chunkPersistenceService,
                        statusService
                );

        documentId =
                UUID.randomUUID();
    }

    @Nested
    class SuccessfulProcessing {

        @Test
        void shouldProcessDocumentSuccessfully() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            String extractedText =
                    "This is extracted document text.";

            List<String> chunks =
                    List.of(
                            "This is extracted",
                            "document text."
                    );

            when(
                    textExtractor.extract(
                            document
                    )
            ).thenReturn(
                    extractedText
            );

            when(
                    textChunker.chunk(
                            extractedText
                    )
            ).thenReturn(
                    chunks
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markProcessing(
                    documentId
            );

            verify(
                    textExtractor
            ).extract(
                    document
            );

            verify(
                    textChunker
            ).chunk(
                    extractedText
            );

            verify(
                    chunkPersistenceService
            ).replaceChunks(
                    documentId,
                    chunks
            );

            verify(
                    statusService
            ).markReady(
                    documentId
            );

            verify(
                    statusService,
                    never()
            ).markFailed(
                    any(),
                    anyString()
            );

            /*
             * Once before validation,
             * once after markProcessing().
             */
            verify(
                    documentRepository,
                    times(2)
            ).findById(
                    documentId
            );
        }

        @Test
        void shouldAllowPendingDocumentToBeProcessed() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            prepareSuccessfulProcessing();

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markReady(
                    documentId
            );
        }
        @Test
        void shouldAllowFailedDocumentToBeProcessedAgain() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.FAILED
            );

            prepareSuccessfulProcessing();

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markReady(
                    documentId
            );
        }
    }

    @Nested
    class StatusValidation {

        @Test
        void shouldRejectDocumentAlreadyProcessing() {
            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PROCESSING
            );

            assertThrows(
                    ConflictException.class,
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService,
                    never()
            ).markProcessing(
                    any()
            );

            verifyNoInteractions(
                    textExtractor
            );

            verifyNoInteractions(
                    textChunker
            );

            verifyNoInteractions(
                    chunkPersistenceService
            );
        }

        @Test
        void shouldRejectDocumentAlreadyReady() {
            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.READY
            );

            assertThrows(
                    ConflictException.class,
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService,
                    never()
            ).markProcessing(
                    any()
            );

            verifyNoInteractions(
                    textExtractor
            );

            verifyNoInteractions(
                    textChunker
            );

            verifyNoInteractions(
                    chunkPersistenceService
            );
        }

        @Test
        void shouldThrowWhenDocumentDoesNotExistInitially() {
            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verifyNoInteractions(
                    statusService
            );

            verifyNoInteractions(
                    textExtractor
            );

            verifyNoInteractions(
                    textChunker
            );

            verifyNoInteractions(
                    chunkPersistenceService
            );
        }
    }

    @Nested
    class ProcessingFailures {

        @Test
        void shouldMarkFailedWhenTextExtractionFails() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            when(
                    textExtractor.extract(
                            document
                    )
            ).thenThrow(
                    new RuntimeException(
                            "Extraction failed"
                    )
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markProcessing(
                    documentId
            );

            verify(
                    statusService
            ).markFailed(
                    documentId,
                    "Extraction failed"
            );

            verify(
                    statusService,
                    never()
            ).markReady(
                    documentId
            );

            verifyNoInteractions(
                    chunkPersistenceService
            );
        }

        @Test
        void shouldMarkFailedWhenChunkingProducesNoChunks() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            String extractedText =
                    "Extracted text";

            when(
                    textExtractor.extract(
                            document
                    )
            ).thenReturn(
                    extractedText
            );

            when(
                    textChunker.chunk(
                            extractedText
                    )
            ).thenReturn(
                    List.of()
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markFailed(
                    documentId,
                    "Document produced no usable text chunks"
            );

            verify(
                    chunkPersistenceService,
                    never()
            ).replaceChunks(
                    any(),
                    anyList()
            );

            verify(
                    statusService,
                    never()
            ).markReady(
                    documentId
            );
        }

        @Test
        void shouldMarkFailedWhenChunkPersistenceFails() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            String extractedText =
                    "Useful extracted text";

            List<String> chunks =
                    List.of(
                            "Useful extracted text"
                    );

            when(
                    textExtractor.extract(
                            document
                    )
            ).thenReturn(
                    extractedText
            );

            when(
                    textChunker.chunk(
                            extractedText
                    )
            ).thenReturn(
                    chunks
            );

            doThrow(
                    new RuntimeException(
                            "Chunk persistence failed"
                    )
            ).when(
                    chunkPersistenceService
            ).replaceChunks(
                    documentId,
                    chunks
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markFailed(
                    documentId,
                    "Chunk persistence failed"
            );

            verify(
                    statusService,
                    never()
            ).markReady(
                    documentId
            );
        }

        @Test
        void shouldMarkFailedWhenDocumentDisappearsAfterProcessingStarts() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    ),
                    Optional.empty()
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markProcessing(
                    documentId
            );

            verify(
                    statusService
            ).markFailed(
                    eq(
                            documentId
                    ),
                    anyString()
            );

            verifyNoInteractions(
                    textExtractor
            );

            verifyNoInteractions(
                    textChunker
            );

            verifyNoInteractions(
                    chunkPersistenceService
            );
        }

        @Test
        void shouldUseExceptionClassNameWhenExceptionMessageIsMissing() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            when(
                    textExtractor.extract(
                            document
                    )
            ).thenThrow(
                    new RuntimeException()
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markFailed(
                    documentId,
                    "RuntimeException"
            );
        }

        @Test
        void shouldUseExceptionClassNameWhenExceptionMessageIsBlank() {
            when(
                    document.getStatus()
            ).thenReturn(
                    DocumentStatus.PENDING
            );

            when(
                    documentRepository.findById(
                            documentId
                    )
            ).thenReturn(
                    Optional.of(
                            document
                    )
            );

            when(
                    textExtractor.extract(
                            document
                    )
            ).thenThrow(
                    new RuntimeException(
                            "   "
                    )
            );

            assertDoesNotThrow(
                    () ->
                            documentProcessingService
                                    .processDocument(
                                            documentId
                                    )
            );

            verify(
                    statusService
            ).markFailed(
                    documentId,
                    "RuntimeException"
            );
        }
    }

    private void prepareSuccessfulProcessing() {
        when(
                documentRepository.findById(
                        documentId
                )
        ).thenReturn(
                Optional.of(
                        document
                )
        );

        String extractedText =
                "Processed document text.";

        List<String> chunks =
                List.of(
                        "Processed document text."
                );

        when(
                textExtractor.extract(
                        document
                )
        ).thenReturn(
                extractedText
        );

        when(
                textChunker.chunk(
                        extractedText
                )
        ).thenReturn(
                chunks
        );
    }
}