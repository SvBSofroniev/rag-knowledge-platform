package src.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import src.common.exception.AiModelResponseException;
import src.common.exception.BadRequestException;
import src.document.dto.DocumentChunkSearchResult;
import src.document.repository.DocumentChunkRepository;
import src.embedding.service.EmbeddingService;
import src.entity.User;
import src.rag.dto.SemanticSearchResponse;
import src.workspace.service.WorkspacePermissionService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock
    private EmbeddingService
            embeddingService;

    @Mock
    private DocumentChunkRepository
            chunkRepository;

    @Mock
    private WorkspacePermissionService
            permissionService;

    @Mock
    private User
            currentUser;

    private SemanticSearchService
            semanticSearchService;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        semanticSearchService =
                new SemanticSearchService(
                        embeddingService,
                        chunkRepository,
                        permissionService
                );

        ReflectionTestUtils.setField(
                semanticSearchService,
                "minimumSimilarity",
                0.35
        );

        ReflectionTestUtils.setField(
                semanticSearchService,
                "fallbackMinimumSimilarity",
                0.20
        );

        workspaceId =
                UUID.randomUUID();
    }

    @Nested
    class Search {

        @Test
        void shouldNormalizeQueryAndUseDefaultLimit() {
            float[] embedding =
                    validEmbedding();

            when(
                    embeddingService.generateEmbedding(
                            "animal facts"
                    )
            ).thenReturn(
                    embedding
            );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    List.of()
            );

            List<SemanticSearchResponse> result =
                    semanticSearchService.search(
                            workspaceId,
                            "   animal facts   ",
                            null,
                            currentUser
                    );

            assertTrue(
                    result.isEmpty()
            );

            verify(
                    permissionService
            ).requireMember(
                    workspaceId,
                    currentUser
            );

            verify(
                    embeddingService
            ).generateEmbedding(
                    "animal facts"
            );

            ArgumentCaptor<String>
                    vectorCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            verify(
                    chunkRepository
            ).findSimilarChunks(
                    eq(workspaceId),
                    vectorCaptor.capture(),
                    eq(5)
            );

            assertValidVectorLiteral(
                    vectorCaptor.getValue()
            );
        }

        @Test
        void shouldCheckPermissionBeforeGeneratingEmbedding() {
            float[] embedding =
                    validEmbedding();

            when(
                    embeddingService.generateEmbedding(
                            "query"
                    )
            ).thenReturn(
                    embedding
            );

            when(
                    chunkRepository.findSimilarChunks(
                            any(),
                            anyString(),
                            anyInt()
                    )
            ).thenReturn(
                    List.of()
            );

            semanticSearchService.search(
                    workspaceId,
                    "query",
                    5,
                    currentUser
            );

            InOrder order =
                    inOrder(
                            permissionService,
                            embeddingService,
                            chunkRepository
                    );

            order.verify(
                    permissionService
            ).requireMember(
                    workspaceId,
                    currentUser
            );

            order.verify(
                    embeddingService
            ).generateEmbedding(
                    "query"
            );

            order.verify(
                    chunkRepository
            ).findSimilarChunks(
                    eq(workspaceId),
                    anyString(),
                    eq(5)
            );
        }

        @Test
        void shouldReturnResultsAboveSimilarityThreshold() {
            prepareEmbedding(
                    "query"
            );

            DocumentChunkSearchResult accepted =
                    createSearchResult(
                            "Relevant content",
                            0.60
                    );

            DocumentChunkSearchResult rejected =
                    createSearchResult(
                            "Weak content",
                            0.70
                    );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    List.of(
                            accepted,
                            rejected
                    )
            );

            List<SemanticSearchResponse> result =
                    semanticSearchService.search(
                            workspaceId,
                            "query",
                            5,
                            currentUser
                    );

            assertEquals(
                    1,
                    result.size()
            );

            assertEquals(
                    "Relevant content",
                    result.get(0)
                            .content()
            );

            /*
             * cosine similarity = 1 - distance
             *
             * 1 - 0.60 = 0.40
             */
            assertEquals(
                    0.40,
                    result.get(0)
                            .similarity(),
                    0.000001
            );

            assertEquals(
                    0.60,
                    result.get(0)
                            .distance(),
                    0.000001
            );
        }

        @Test
        void shouldReturnEmptyListWhenNoDatabaseResultsExist() {
            prepareEmbedding(
                    "query"
            );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    List.of()
            );

            List<SemanticSearchResponse> result =
                    semanticSearchService.search(
                            workspaceId,
                            "query",
                            5,
                            currentUser
                    );

            assertTrue(
                    result.isEmpty()
            );
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNull() {
            prepareEmbedding(
                    "query"
            );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    null
            );

            List<SemanticSearchResponse> result =
                    semanticSearchService.search(
                            workspaceId,
                            "query",
                            5,
                            currentUser
                    );

            assertTrue(
                    result.isEmpty()
            );
        }
    }

    @Nested
    class QueryValidation {

        @Test
        void shouldRejectNullQuery() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    null,
                                    5,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    permissionService,
                    embeddingService,
                    chunkRepository
            );
        }

        @Test
        void shouldRejectBlankQuery() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "   ",
                                    5,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    permissionService,
                    embeddingService,
                    chunkRepository
            );
        }

        @Test
        void shouldRejectQueryLongerThan5000Characters() {
            String query =
                    "a".repeat(
                            5001
                    );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    query,
                                    5,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    permissionService,
                    embeddingService,
                    chunkRepository
            );
        }
    }

    @Nested
    class LimitValidation {

        @Test
        void shouldRejectLimitBelowOne() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    0,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    permissionService,
                    embeddingService,
                    chunkRepository
            );
        }

        @Test
        void shouldRejectLimitAboveTwenty() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    21,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    permissionService,
                    embeddingService,
                    chunkRepository
            );
        }
    }

    @Nested
    class EmbeddingValidation {

        @Test
        void shouldRejectNullEmbedding() {
            when(
                    embeddingService.generateEmbedding(
                            "query"
                    )
            ).thenReturn(
                    null
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );

            verify(
                    permissionService
            ).requireMember(
                    workspaceId,
                    currentUser
            );

            verifyNoInteractions(
                    chunkRepository
            );
        }

        @Test
        void shouldRejectEmbeddingWithWrongDimensions() {
            when(
                    embeddingService.generateEmbedding(
                            "query"
                    )
            ).thenReturn(
                    new float[100]
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    chunkRepository
            );
        }

        @Test
        void shouldRejectEmbeddingContainingNaN() {
            float[] embedding =
                    validEmbedding();

            embedding[100] =
                    Float.NaN;

            when(
                    embeddingService.generateEmbedding(
                            "query"
                    )
            ).thenReturn(
                    embedding
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    chunkRepository
            );
        }

        @Test
        void shouldRejectEmbeddingContainingInfinity() {
            float[] embedding =
                    validEmbedding();

            embedding[200] =
                    Float.POSITIVE_INFINITY;

            when(
                    embeddingService.generateEmbedding(
                            "query"
                    )
            ).thenReturn(
                    embedding
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );

            verifyNoInteractions(
                    chunkRepository
            );
        }
    }

    @Nested
    class SelectedDocumentSearch {

        @Test
        void shouldSearchOnlySelectedDocuments() {
            UUID documentOne =
                    UUID.randomUUID();

            UUID documentTwo =
                    UUID.randomUUID();

            prepareEmbedding(
                    "query"
            );

            when(
                    chunkRepository
                            .findSimilarChunksInDocuments(
                                    eq(workspaceId),
                                    eq(
                                            List.of(
                                                    documentOne,
                                                    documentTwo
                                            )
                                    ),
                                    anyString(),
                                    eq(5)
                            )
            ).thenReturn(
                    List.of()
            );

            semanticSearchService
                    .searchInDocuments(
                            workspaceId,
                            List.of(
                                    documentOne,
                                    documentTwo
                            ),
                            "query",
                            5,
                            currentUser
                    );

            verify(
                    chunkRepository
            ).findSimilarChunksInDocuments(
                    eq(workspaceId),
                    eq(
                            List.of(
                                    documentOne,
                                    documentTwo
                            )
                    ),
                    anyString(),
                    eq(5)
            );
        }

        @Test
        void shouldRemoveNullAndDuplicateDocumentIds() {
            UUID documentOne =
                    UUID.randomUUID();

            UUID documentTwo =
                    UUID.randomUUID();

            prepareEmbedding(
                    "query"
            );

            when(
                    chunkRepository
                            .findSimilarChunksInDocuments(
                                    eq(workspaceId),
                                    eq(
                                            List.of(
                                                    documentOne,
                                                    documentTwo
                                            )
                                    ),
                                    anyString(),
                                    eq(5)
                            )
            ).thenReturn(
                    List.of()
            );

            semanticSearchService
                    .searchInDocuments(
                            workspaceId,
                            java.util.Arrays.asList(
                                    documentOne,
                                    null,
                                    documentOne,
                                    documentTwo
                            ),
                            "query",
                            5,
                            currentUser
                    );

            verify(
                    chunkRepository
            ).findSimilarChunksInDocuments(
                    eq(workspaceId),
                    eq(
                            List.of(
                                    documentOne,
                                    documentTwo
                            )
                    ),
                    anyString(),
                    eq(5)
            );
        }

        @Test
        void shouldRejectEmptyDocumentSelection() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService
                                    .searchInDocuments(
                                            workspaceId,
                                            List.of(),
                                            "query",
                                            5,
                                            currentUser
                                    )
            );

            verify(
                    permissionService
            ).requireMember(
                    workspaceId,
                    currentUser
            );

            verifyNoInteractions(
                    embeddingService,
                    chunkRepository
            );
        }

        @Test
        void shouldRejectSelectionContainingOnlyNullIds() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService
                                    .searchInDocuments(
                                            workspaceId,
                                            java.util.Arrays.asList(
                                                    null,
                                                    null
                                            ),
                                            "query",
                                            5,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    embeddingService,
                    chunkRepository
            );
        }

        @Test
        void normalSelectedDocumentSearchShouldRemainStrict() {
            UUID documentId =
                    UUID.randomUUID();

            prepareEmbedding(
                    "query"
            );

            /*
             * distance 0.75
             * similarity = 0.25
             *
             * Below normal threshold 0.35,
             * above fallback threshold 0.20.
             */
            DocumentChunkSearchResult result =
                    createSearchResult(
                            "Broad contextual result",
                            0.75
                    );

            when(
                    chunkRepository
                            .findSimilarChunksInDocuments(
                                    eq(workspaceId),
                                    eq(
                                            List.of(
                                                    documentId
                                            )
                                    ),
                                    anyString(),
                                    eq(5)
                            )
            ).thenReturn(
                    List.of(
                            result
                    )
            );

            List<SemanticSearchResponse> results =
                    semanticSearchService
                            .searchInDocuments(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    "query",
                                    5,
                                    currentUser
                            );

            assertTrue(
                    results.isEmpty()
            );
        }

        @Test
        void fallbackSearchShouldAcceptSofterSimilarityThreshold() {
            UUID documentId =
                    UUID.randomUUID();

            prepareEmbedding(
                    "query"
            );

            DocumentChunkSearchResult result =
                    createSearchResult(
                            "Broad contextual result",
                            0.75
                    );

            when(
                    chunkRepository
                            .findSimilarChunksInDocuments(
                                    eq(workspaceId),
                                    eq(
                                            List.of(
                                                    documentId
                                            )
                                    ),
                                    anyString(),
                                    eq(5)
                            )
            ).thenReturn(
                    List.of(
                            result
                    )
            );

            List<SemanticSearchResponse> results =
                    semanticSearchService
                            .searchInDocumentsWithFallback(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    "query",
                                    5,
                                    currentUser
                            );

            assertEquals(
                    1,
                    results.size()
            );

            assertEquals(
                    0.25,
                    results.get(0)
                            .similarity(),
                    0.000001
            );
        }

        @Test
        void fallbackSearchShouldRejectResultsBelowFallbackThreshold() {
            UUID documentId =
                    UUID.randomUUID();

            prepareEmbedding(
                    "query"
            );

            /*
             * distance 0.85
             * similarity 0.15
             */
            DocumentChunkSearchResult result =
                    createSearchResult(
                            "Very weak result",
                            0.85
                    );

            when(
                    chunkRepository
                            .findSimilarChunksInDocuments(
                                    eq(workspaceId),
                                    anyList(),
                                    anyString(),
                                    eq(5)
                            )
            ).thenReturn(
                    List.of(
                            result
                    )
            );

            List<SemanticSearchResponse> results =
                    semanticSearchService
                            .searchInDocumentsWithFallback(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    "query",
                                    5,
                                    currentUser
                            );

            assertTrue(
                    results.isEmpty()
            );
        }

        @Test
        void fallbackShouldNotBeUsedWhenStrictResultsExist() {
            UUID documentId =
                    UUID.randomUUID();

            prepareEmbedding(
                    "query"
            );

            DocumentChunkSearchResult strict =
                    createSearchResult(
                            "Strong result",
                            0.50
                    );

            DocumentChunkSearchResult fallbackOnly =
                    createSearchResult(
                            "Fallback result",
                            0.75
                    );

            when(
                    chunkRepository
                            .findSimilarChunksInDocuments(
                                    eq(workspaceId),
                                    anyList(),
                                    anyString(),
                                    eq(5)
                            )
            ).thenReturn(
                    List.of(
                            strict,
                            fallbackOnly
                    )
            );

            List<SemanticSearchResponse> results =
                    semanticSearchService
                            .searchInDocumentsWithFallback(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    "query",
                                    5,
                                    currentUser
                            );

            assertEquals(
                    1,
                    results.size()
            );

            assertEquals(
                    "Strong result",
                    results.get(0)
                            .content()
            );
        }
    }

    @Nested
    class DocumentContext {

        @Test
        void shouldReturnDocumentContextWithoutSimilarityValues() {
            UUID documentId =
                    UUID.randomUUID();

            DocumentChunkSearchResult result =
                    createContextResult(
                            "Full document context"
                    );

            when(
                    chunkRepository
                            .findDocumentContextChunks(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    5
                            )
            ).thenReturn(
                    List.of(
                            result
                    )
            );

            List<SemanticSearchResponse> responses =
                    semanticSearchService
                            .getDocumentContext(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    null,
                                    currentUser
                            );

            assertEquals(
                    1,
                    responses.size()
            );

            SemanticSearchResponse response =
                    responses.get(0);

            assertEquals(
                    "Full document context",
                    response.content()
            );

            assertNull(
                    response.distance()
            );

            assertNull(
                    response.similarity()
            );

            verifyNoInteractions(
                    embeddingService
            );
        }

        @Test
        void shouldUseMaximumConfiguredContextLimit() {
            UUID documentId =
                    UUID.randomUUID();

            when(
                    chunkRepository
                            .findDocumentContextChunks(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    100
                            )
            ).thenReturn(
                    List.of()
            );

            assertDoesNotThrow(
                    () ->
                            semanticSearchService
                                    .getDocumentContext(
                                            workspaceId,
                                            List.of(
                                                    documentId
                                            ),
                                            100,
                                            currentUser
                                    )
            );
        }

        @Test
        void shouldRejectContextLimitBelowOne() {
            UUID documentId =
                    UUID.randomUUID();

            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService
                                    .getDocumentContext(
                                            workspaceId,
                                            List.of(
                                                    documentId
                                            ),
                                            0,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    chunkRepository
            );
        }

        @Test
        void shouldRejectContextLimitAboveOneHundred() {
            UUID documentId =
                    UUID.randomUUID();

            assertThrows(
                    BadRequestException.class,
                    () ->
                            semanticSearchService
                                    .getDocumentContext(
                                            workspaceId,
                                            List.of(
                                                    documentId
                                            ),
                                            101,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    chunkRepository
            );
        }

        @Test
        void shouldRejectInvalidContextResult() {
            UUID documentId =
                    UUID.randomUUID();

            when(
                    chunkRepository
                            .findDocumentContextChunks(
                                    workspaceId,
                                    List.of(
                                            documentId
                                    ),
                                    5
                            )
            ).thenReturn(
                    java.util.Collections
                            .singletonList(
                                    null
                            )
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService
                                    .getDocumentContext(
                                            workspaceId,
                                            List.of(
                                                    documentId
                                            ),
                                            5,
                                            currentUser
                                    )
            );
        }
    }

    @Nested
    class SearchResultValidation {

        @Test
        void shouldRejectNullSemanticSearchResult() {
            prepareEmbedding(
                    "query"
            );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    java.util.Collections
                            .singletonList(
                                    null
                            )
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );
        }

        @Test
        void shouldRejectResultWithNullDistance() {
            prepareEmbedding(
                    "query"
            );

            DocumentChunkSearchResult result =
                    mock(
                            DocumentChunkSearchResult.class
                    );

            when(
                    result.getDistance()
            ).thenReturn(
                    null
            );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    List.of(
                            result
                    )
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );
        }

        @Test
        void shouldRejectResultWithNonFiniteDistance() {
            prepareEmbedding(
                    "query"
            );

            DocumentChunkSearchResult result =
                    mock(
                            DocumentChunkSearchResult.class
                    );

            when(
                    result.getDistance()
            ).thenReturn(
                    Double.NaN
            );

            when(
                    chunkRepository.findSimilarChunks(
                            eq(workspaceId),
                            anyString(),
                            eq(5)
                    )
            ).thenReturn(
                    List.of(
                            result
                    )
            );

            assertThrows(
                    AiModelResponseException.class,
                    () ->
                            semanticSearchService.search(
                                    workspaceId,
                                    "query",
                                    5,
                                    currentUser
                            )
            );
        }
    }

    private void prepareEmbedding(
            String query
    ) {
        when(
                embeddingService.generateEmbedding(
                        query
                )
        ).thenReturn(
                validEmbedding()
        );
    }

    private float[] validEmbedding() {
        float[] embedding =
                new float[768];

        for (
                int index = 0;
                index < embedding.length;
                index++
        ) {
            embedding[index] =
                    index / 10_000.0f;
        }

        return embedding;
    }

    private DocumentChunkSearchResult
    createSearchResult(
            String content,
            double distance
    ) {
        DocumentChunkSearchResult result =
                mock(
                        DocumentChunkSearchResult.class
                );

        when(
                result.getChunkId()
        ).thenReturn(
                UUID.randomUUID()
        );

        when(
                result.getDocumentId()
        ).thenReturn(
                UUID.randomUUID()
        );

        when(
                result.getDocumentTitle()
        ).thenReturn(
                "Test Document"
        );

        when(
                result.getChunkIndex()
        ).thenReturn(
                0
        );

        when(
                result.getContent()
        ).thenReturn(
                content
        );

        when(
                result.getDistance()
        ).thenReturn(
                distance
        );

        return result;
    }

    private DocumentChunkSearchResult
    createContextResult(
            String content
    ) {
        DocumentChunkSearchResult result =
                mock(
                        DocumentChunkSearchResult.class
                );

        when(
                result.getChunkId()
        ).thenReturn(
                UUID.randomUUID()
        );

        when(
                result.getDocumentId()
        ).thenReturn(
                UUID.randomUUID()
        );

        when(
                result.getDocumentTitle()
        ).thenReturn(
                "Context Document"
        );

        when(
                result.getChunkIndex()
        ).thenReturn(
                0
        );

        when(
                result.getContent()
        ).thenReturn(
                content
        );

        return result;
    }

    private void assertValidVectorLiteral(
            String vector
    ) {
        assertNotNull(
                vector
        );

        assertTrue(
                vector.startsWith(
                        "["
                )
        );

        assertTrue(
                vector.endsWith(
                        "]"
                )
        );

        String contents =
                vector.substring(
                        1,
                        vector.length() - 1
                );

        String[] values =
                contents.split(
                        ","
                );

        assertEquals(
                768,
                values.length
        );
    }
}