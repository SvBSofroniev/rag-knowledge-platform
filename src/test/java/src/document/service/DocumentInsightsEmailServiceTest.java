package src.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.common.exception.BadRequestException;
import src.document.dto.DocumentDetailsResponse;
import src.document.dto.EmailDocumentInsightsRequest;
import src.document.util.DocumentStatus;
import src.entity.User;
import src.mail.service.MailService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentInsightsEmailServiceTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private MailService mailService;

    @Mock
    private User currentUser;

    @Mock
    private DocumentDetailsResponse document;

    @Mock
    private EmailDocumentInsightsRequest request;

    private DocumentInsightsEmailService
            documentInsightsEmailService;

    private UUID documentId;

    @BeforeEach
    void setUp() {
        documentInsightsEmailService =
                new DocumentInsightsEmailService(
                        documentService,
                        mailService
                );

        documentId =
                UUID.randomUUID();
    }

    @Nested
    class AccessAndStatus {

        @Test
        void shouldVerifyDocumentAccessBeforeSendingEmail() {
            prepareEnglishEmail();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(documentService)
                    .getDocumentDetails(
                            documentId,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            anyString(),
                            anyString()
                    );
        }

        @Test
        void shouldRejectDocumentThatIsNotReady() {
            when(
                    documentService.getDocumentDetails(
                            documentId,
                            currentUser
                    )
            ).thenReturn(
                    document
            );

            when(
                    document.status()
            ).thenReturn(
                    DocumentStatus.PROCESSING
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            documentInsightsEmailService
                                    .sendInsights(
                                            documentId,
                                            request,
                                            currentUser
                                    )
            );

            verify(documentService)
                    .getDocumentDetails(
                            documentId,
                            currentUser
                    );

            verifyNoInteractions(
                    mailService
            );

            verify(
                    currentUser,
                    never()
            ).getEmail();
        }
    }

    @Nested
    class Recipient {

        @Test
        void shouldAlwaysSendToCurrentUsersEmail() {
            prepareEnglishEmail();

            when(
                    currentUser.getEmail()
            ).thenReturn(
                    "authenticated-user@example.com"
            );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            eq(
                                    "authenticated-user@example.com"
                            ),
                            anyString(),
                            anyString()
                    );
        }
    }

    @Nested
    class EnglishEmail {

        @Test
        void shouldBuildEnglishSubject() {
            prepareEnglishEmail();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            eq(
                                    "OurVault AI Insights - Test Document"
                            ),
                            anyString()
                    );
        }

        @Test
        void shouldBuildEnglishEmailBody() {
            prepareEnglishEmail();

            ArgumentCaptor<String> bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture()
                    );

            String body =
                    bodyCaptor.getValue();

            assertTrue(
                    body.contains(
                            "Hello svetlin,"
                    )
            );

            assertTrue(
                    body.contains(
                            "Here are the AI insights generated by OurVault."
                    )
            );

            assertTrue(
                    body.contains(
                            "Document: Test Document"
                    )
            );

            assertTrue(
                    body.contains(
                            "Workspace: Master Workspace"
                    )
            );

            assertTrue(
                    body.contains(
                            "SUMMARY"
                    )
            );

            assertTrue(
                    body.contains(
                            "This is the document summary."
                    )
            );

            assertTrue(
                    body.contains(
                            "KEY POINTS"
                    )
            );

            assertTrue(
                    body.contains(
                            "1. First key point"
                    )
            );

            assertTrue(
                    body.contains(
                            "2. Second key point"
                    )
            );

            assertTrue(
                    body.contains(
                            "IMPORTANT FACTS"
                    )
            );

            assertTrue(
                    body.contains(
                            "1. First important fact"
                    )
            );

            assertTrue(
                    body.contains(
                            "2. Second important fact"
                    )
            );

            assertTrue(
                    body.contains(
                            "Generated by OurVault"
                    )
            );
        }

        @Test
        void shouldDefaultToEnglishWhenLanguageIsNull() {
            prepareCommonReadyDocument();

            when(
                    request.language()
            ).thenReturn(
                    null
            );

            prepareRequestContent();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            eq(
                                    "OurVault AI Insights - Test Document"
                            ),
                            contains(
                                    "SUMMARY"
                            )
                    );
        }

        @Test
        void shouldDefaultToEnglishForUnknownLanguage() {
            prepareCommonReadyDocument();

            when(
                    request.language()
            ).thenReturn(
                    "de"
            );

            prepareRequestContent();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            eq(
                                    "OurVault AI Insights - Test Document"
                            ),
                            contains(
                                    "KEY POINTS"
                            )
                    );
        }
    }

    @Nested
    class BulgarianEmail {

        @Test
        void shouldBuildBulgarianSubject() {
            prepareBulgarianEmail();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            eq(
                                    "OurVault AI анализ - Test Document"
                            ),
                            anyString()
                    );
        }

        @Test
        void shouldBuildBulgarianEmailBody() {
            prepareBulgarianEmail();

            ArgumentCaptor<String> bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture()
                    );

            String body =
                    bodyCaptor.getValue();

            assertTrue(
                    body.contains(
                            "Здравейте, svetlin!"
                    )
            );

            assertTrue(
                    body.contains(
                            "Ето AI анализа, генериран от OurVault."
                    )
            );

            assertTrue(
                    body.contains(
                            "Документ: Test Document"
                    )
            );

            assertTrue(
                    body.contains(
                            "Работно пространство: Master Workspace"
                    )
            );

            assertTrue(
                    body.contains(
                            "ОБОБЩЕНИЕ"
                    )
            );

            assertTrue(
                    body.contains(
                            "This is the document summary."
                    )
            );

            assertTrue(
                    body.contains(
                            "КЛЮЧОВИ ТОЧКИ"
                    )
            );

            assertTrue(
                    body.contains(
                            "1. First key point"
                    )
            );

            assertTrue(
                    body.contains(
                            "ВАЖНИ ФАКТИ"
                    )
            );

            assertTrue(
                    body.contains(
                            "1. First important fact"
                    )
            );

            assertTrue(
                    body.contains(
                            "Генерирано от OurVault"
                    )
            );
        }

        @Test
        void shouldRecognizeBulgarianLocaleVariant() {
            prepareCommonReadyDocument();

            when(
                    request.language()
            ).thenReturn(
                    "  BG-bg  "
            );

            prepareRequestContent();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            eq(
                                    "OurVault AI анализ - Test Document"
                            ),
                            contains(
                                    "ОБОБЩЕНИЕ"
                            )
                    );
        }
    }

    @Nested
    class InsightFormatting {

        @Test
        void shouldTrimSummaryAndListItems() {
            prepareCommonReadyDocument();

            when(
                    request.language()
            ).thenReturn(
                    "en"
            );

            when(
                    request.summary()
            ).thenReturn(
                    "   Trimmed summary   "
            );

            when(
                    request.keyPoints()
            ).thenReturn(
                    List.of(
                            "   First point   ",
                            "   Second point   "
                    )
            );

            when(
                    request.importantFacts()
            ).thenReturn(
                    List.of(
                            "   Important fact   "
                    )
            );

            ArgumentCaptor<String> bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture()
                    );

            String body =
                    bodyCaptor.getValue();

            assertTrue(
                    body.contains(
                            "Trimmed summary"
                    )
            );

            assertTrue(
                    body.contains(
                            "1. First point"
                    )
            );

            assertTrue(
                    body.contains(
                            "2. Second point"
                    )
            );

            assertTrue(
                    body.contains(
                            "1. Important fact"
                    )
            );

            assertFalse(
                    body.contains(
                            "   Trimmed summary   "
                    )
            );
        }

        @Test
        void shouldUseDashWhenKeyPointsAreEmpty() {
            prepareCommonReadyDocument();

            when(
                    request.language()
            ).thenReturn(
                    "en"
            );

            when(
                    request.summary()
            ).thenReturn(
                    "Summary"
            );

            when(
                    request.keyPoints()
            ).thenReturn(
                    List.of()
            );

            when(
                    request.importantFacts()
            ).thenReturn(
                    List.of(
                            "Fact"
                    )
            );

            ArgumentCaptor<String> bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture()
                    );

            String body =
                    bodyCaptor.getValue();

            assertTrue(
                    body.contains(
                            """
                            KEY POINTS
                            --------------------
                            -
                            """
                    )
            );
        }

        @Test
        void shouldUseDashWhenImportantFactsAreNull() {
            prepareCommonReadyDocument();

            when(
                    request.language()
            ).thenReturn(
                    "en"
            );

            when(
                    request.summary()
            ).thenReturn(
                    "Summary"
            );

            when(
                    request.keyPoints()
            ).thenReturn(
                    List.of(
                            "Point"
                    )
            );

            when(
                    request.importantFacts()
            ).thenReturn(
                    null
            );

            ArgumentCaptor<String> bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendTextEmail(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture()
                    );

            String body =
                    bodyCaptor.getValue();

            assertTrue(
                    body.contains(
                            """
                            IMPORTANT FACTS
                            --------------------
                            -
                            """
                    )
            );
        }
    }

    private void prepareEnglishEmail() {
        prepareCommonReadyDocument();

        when(
                request.language()
        ).thenReturn(
                "en"
        );

        prepareRequestContent();
    }

    private void prepareBulgarianEmail() {
        prepareCommonReadyDocument();

        when(
                request.language()
        ).thenReturn(
                "bg"
        );

        prepareRequestContent();
    }

    private void prepareCommonReadyDocument() {
        when(
                documentService.getDocumentDetails(
                        documentId,
                        currentUser
                )
        ).thenReturn(
                document
        );

        when(
                document.status()
        ).thenReturn(
                DocumentStatus.READY
        );

        when(
                document.title()
        ).thenReturn(
                "Test Document"
        );

        when(
                document.workspaceName()
        ).thenReturn(
                "Master Workspace"
        );

        when(
                currentUser.getEmail()
        ).thenReturn(
                "svetlin@example.com"
        );

        when(
                currentUser.getUsername()
        ).thenReturn(
                "svetlin"
        );
    }

    private void prepareRequestContent() {
        when(
                request.summary()
        ).thenReturn(
                "This is the document summary."
        );

        when(
                request.keyPoints()
        ).thenReturn(
                List.of(
                        "First key point",
                        "Second key point"
                )
        );

        when(
                request.importantFacts()
        ).thenReturn(
                List.of(
                        "First important fact",
                        "Second important fact"
                )
        );
    }
}