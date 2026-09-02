package src.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private TextChunker textChunker;

    @BeforeEach
    void setUp() {
        textChunker =
                new TextChunker();
    }

    @Nested
    class EmptyInput {

        @Test
        void shouldReturnEmptyListForNullText() {
            List<String> result =
                    textChunker.chunk(
                            null
                    );

            assertTrue(
                    result.isEmpty()
            );
        }

        @Test
        void shouldReturnEmptyListForBlankText() {
            List<String> result =
                    textChunker.chunk(
                            "   \n\t   "
                    );

            assertTrue(
                    result.isEmpty()
            );
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldRejectZeroChunkSize() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            textChunker.chunk(
                                    "Hello",
                                    0,
                                    0
                            )
            );
        }

        @Test
        void shouldRejectNegativeChunkSize() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            textChunker.chunk(
                                    "Hello",
                                    -1,
                                    0
                            )
            );
        }

        @Test
        void shouldRejectNegativeOverlap() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            textChunker.chunk(
                                    "Hello",
                                    10,
                                    -1
                            )
            );
        }

        @Test
        void shouldRejectOverlapEqualToChunkSize() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            textChunker.chunk(
                                    "Hello",
                                    10,
                                    10
                            )
            );
        }

        @Test
        void shouldRejectOverlapGreaterThanChunkSize() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            textChunker.chunk(
                                    "Hello",
                                    10,
                                    11
                            )
            );
        }
    }

    @Nested
    class Normalization {

        @Test
        void shouldNormalizeWhitespaceAndPreserveParagraphBreaks() {
            String text =
                    "  First   line\r\n" +
                            "wrapped\ttext\r\n\r\n" +
                            "Second   paragraph  ";

            List<String> result =
                    textChunker.chunk(
                            text,
                            500,
                            0
                    );

            assertEquals(
                    1,
                    result.size()
            );

            assertEquals(
                    "First line wrapped text\n\nSecond paragraph",
                    result.get(0)
            );
        }

        @Test
        void shouldJoinSingleVisualLineBreaks() {
            String text =
                    """
                    This text was
                    visually wrapped
                    across several lines.
                    """;

            List<String> result =
                    textChunker.chunk(
                            text,
                            500,
                            0
                    );

            assertEquals(
                    List.of(
                            "This text was visually wrapped across several lines."
                    ),
                    result
            );
        }
    }

    @Nested
    class Chunking {

        @Test
        void shouldReturnSingleChunkWhenTextFits() {
            List<String> result =
                    textChunker.chunk(
                            "This is a short document.",
                            100,
                            20
                    );

            assertEquals(
                    1,
                    result.size()
            );

            assertEquals(
                    "This is a short document.",
                    result.get(0)
            );
        }

        @Test
        void shouldPreferParagraphBoundary() {
            String text =
                    "abcdefgh\n\nijklmnop";

            List<String> result =
                    textChunker.chunk(
                            text,
                            10,
                            0
                    );

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    "abcdefgh",
                    result.get(0)
            );

            assertEquals(
                    "ijklmnop",
                    result.get(1)
            );
        }

        @Test
        void shouldPreferSentenceBoundaryWhenAvailable() {
            String text =
                    "1234567. abcdefgh";

            List<String> result =
                    textChunker.chunk(
                            text,
                            10,
                            0
                    );

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    "1234567.",
                    result.get(0)
            );

            assertEquals(
                    "abcdefgh",
                    result.get(1)
            );
        }

        @Test
        void shouldUseWhitespaceBoundaryWhenNoSentenceBoundaryExists() {
            String text =
                    "abcdefgh ijklmnop";

            List<String> result =
                    textChunker.chunk(
                            text,
                            10,
                            0
                    );

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    "abcdefgh",
                    result.get(0)
            );

            assertEquals(
                    "ijklmnop",
                    result.get(1)
            );
        }

        @Test
        void shouldHardSplitWhenNoNaturalBoundaryExists() {
            String text =
                    "abcdefghijklmnop";

            List<String> result =
                    textChunker.chunk(
                            text,
                            10,
                            0
                    );

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    "abcdefghij",
                    result.get(0)
            );

            assertEquals(
                    "klmnop",
                    result.get(1)
            );
        }

        @Test
        void shouldApplyConfiguredOverlap() {
            String text =
                    "abcdefghijklmnopqrst";

            List<String> result =
                    textChunker.chunk(
                            text,
                            10,
                            3
                    );

            assertEquals(
                    3,
                    result.size()
            );

            assertEquals(
                    "abcdefghij",
                    result.get(0)
            );

            assertEquals(
                    "hijklmnopq",
                    result.get(1)
            );

            assertEquals(
                    "opqrst",
                    result.get(2)
            );

            /*
             * Last 3 characters from the first
             * chunk become the beginning of the
             * next chunk.
             */
            assertTrue(
                    result.get(1)
                            .startsWith(
                                    "hij"
                            )
            );
        }

        @Test
        void shouldNeverCreateBlankChunks() {
            String text =
                    "First paragraph\n\n\n\nSecond paragraph";

            List<String> result =
                    textChunker.chunk(
                            text,
                            15,
                            2
                    );

            assertFalse(
                    result.isEmpty()
            );

            assertTrue(
                    result.stream()
                            .noneMatch(
                                    String::isBlank
                            )
            );
        }
    }

    @Nested
    class DefaultConfiguration {

        @Test
        void shouldSplitLargeTextUsingDefaultConfiguration() {
            StringBuilder text =
                    new StringBuilder();

            for (
                    int i = 0;
                    i < 1000;
                    i++
            ) {
                text.append(
                        "OurVault stores and retrieves document knowledge. "
                );
            }

            List<String> result =
                    textChunker.chunk(
                            text.toString()
                    );

            assertTrue(
                    result.size() > 1
            );

            assertTrue(
                    result.stream()
                            .allMatch(
                                    chunk ->
                                            chunk.length()
                                                    <= 1500
                            )
            );
        }

        @Test
        void shouldPreserveAllMajorPartsOfLargeText() {
            String text =
                    """
                    Introduction paragraph containing useful information.

                    Architecture paragraph describing the OurVault system architecture.

                    Retrieval paragraph describing embeddings and semantic search.

                    Conclusion paragraph containing the final observations.
                    """;

            List<String> result =
                    textChunker.chunk(
                            text,
                            100,
                            20
                    );

            String combined =
                    String.join(
                            " ",
                            result
                    );

            assertTrue(
                    combined.contains(
                            "Introduction"
                    )
            );

            assertTrue(
                    combined.contains(
                            "Architecture"
                    )
            );

            assertTrue(
                    combined.contains(
                            "Retrieval"
                    )
            );

            assertTrue(
                    combined.contains(
                            "Conclusion"
                    )
            );
        }
    }
}