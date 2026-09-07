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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentInsightsEmailServiceTest {

    private static final byte[] PDF_CONTENT =
            new byte[]{
                    1,
                    2,
                    3,
                    4
            };

    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentInsightsPdfService
            documentInsightsPdfService;

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
                        documentInsightsPdfService,
                        mailService
                );

        documentId =
                UUID.randomUUID();
    }

    /*
     * ---------------------------------------------------------
     * ACCESS AND STATUS
     * ---------------------------------------------------------
     */
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

            verify(documentInsightsPdfService)
                    .generate(
                            document,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            anyString(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
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
                    documentInsightsPdfService,
                    mailService
            );

            verify(
                    currentUser,
                    never()
            ).getEmail();
        }
    }

    /*
     * ---------------------------------------------------------
     * RECIPIENT
     * ---------------------------------------------------------
     */
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
                    .sendEmailWithPdfAttachment(
                            eq(
                                    "authenticated-user@example.com"
                            ),
                            anyString(),
                            anyString(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
                    );
        }
    }

    /*
     * ---------------------------------------------------------
     * PDF GENERATION
     * ---------------------------------------------------------
     */
    @Nested
    class PdfGeneration {

        @Test
        void shouldGeneratePdfUsingExistingInsightsRequest() {

            prepareEnglishEmail();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(documentInsightsPdfService)
                    .generate(
                            same(
                                    document
                            ),
                            same(
                                    request
                            ),
                            same(
                                    currentUser
                            )
                    );
        }

        @Test
        void shouldSendGeneratedPdfAsAttachment() {

            prepareEnglishEmail();

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            anyString(),
                            eq(
                                    "OurVault_Test_Document_Insights.pdf"
                            ),
                            same(
                                    PDF_CONTENT
                            )
                    );
        }

        @Test
        void shouldCreateSafeAttachmentFilename() {

            prepareEnglishEmail();

            when(
                    document.title()
            ).thenReturn(
                    "Security Report: 2026 / Final"
            );

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            anyString(),
                            eq(
                                    "OurVault_Security_Report_2026_Final_Insights.pdf"
                            ),
                            same(
                                    PDF_CONTENT
                            )
                    );
        }
    }

    /*
     * ---------------------------------------------------------
     * ENGLISH EMAIL
     * ---------------------------------------------------------
     */
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
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            eq(
                                    "OurVault AI Insights - Test Document"
                            ),
                            anyString(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
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
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
                    );

            String body =
                    bodyCaptor.getValue();

            assertTrue(
                    body.contains(
                            "Hello svetlin!"
                    )
            );

            assertTrue(
                    body.contains(
                            "The AI insights for \"Test Document\" are ready."
                    )
            );

            assertTrue(
                    body.contains(
                            "Workspace: Master Workspace"
                    )
            );

            assertTrue(
                    body.contains(
                            "A PDF report is attached containing:"
                    )
            );

            assertTrue(
                    body.contains(
                            "document summary"
                    )
            );

            assertTrue(
                    body.contains(
                            "key points"
                    )
            );

            assertTrue(
                    body.contains(
                            "important facts"
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

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            eq(
                                    "OurVault AI Insights - Test Document"
                            ),
                            contains(
                                    "The AI insights for"
                            ),
                            anyString(),
                            same(
                                    PDF_CONTENT
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

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            eq(
                                    "OurVault AI Insights - Test Document"
                            ),
                            contains(
                                    "A PDF report is attached"
                            ),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
                    );
        }
    }

    /*
     * ---------------------------------------------------------
     * BULGARIAN EMAIL
     * ---------------------------------------------------------
     */
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
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            eq(
                                    "OurVault AI анализ - Test Document"
                            ),
                            anyString(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
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
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
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
                            "AI анализът за документа „Test Document“ е готов."
                    )
            );

            assertTrue(
                    body.contains(
                            "Работно пространство: Master Workspace"
                    )
            );

            assertTrue(
                    body.contains(
                            "Към този имейл е приложен PDF отчет"
                    )
            );

            assertTrue(
                    body.contains(
                            "обобщение на документа"
                    )
            );

            assertTrue(
                    body.contains(
                            "ключови точки"
                    )
            );

            assertTrue(
                    body.contains(
                            "важни факти"
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

            documentInsightsEmailService
                    .sendInsights(
                            documentId,
                            request,
                            currentUser
                    );

            verify(mailService)
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            eq(
                                    "OurVault AI анализ - Test Document"
                            ),
                            contains(
                                    "Към този имейл е приложен PDF отчет"
                            ),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
                    );
        }
    }

    /*
     * ---------------------------------------------------------
     * USERNAME FALLBACK
     * ---------------------------------------------------------
     */
    @Nested
    class UsernameFallback {

        @Test
        void shouldUseEmailWhenUsernameIsBlank() {

            prepareEnglishEmail();

            when(
                    currentUser.getUsername()
            ).thenReturn(
                    "   "
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
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
                    );

            assertTrue(
                    bodyCaptor
                            .getValue()
                            .contains(
                                    "Hello svetlin@example.com!"
                            )
            );
        }

        @Test
        void shouldUseEmailWhenUsernameIsNull() {

            prepareEnglishEmail();

            when(
                    currentUser.getUsername()
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
                    .sendEmailWithPdfAttachment(
                            anyString(),
                            anyString(),
                            bodyCaptor.capture(),
                            anyString(),
                            same(
                                    PDF_CONTENT
                            )
                    );

            assertTrue(
                    bodyCaptor
                            .getValue()
                            .contains(
                                    "Hello svetlin@example.com!"
                            )
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * HELPERS
     * ---------------------------------------------------------
     */
    private void prepareEnglishEmail() {

        prepareCommonReadyDocument();

        when(
                request.language()
        ).thenReturn(
                "en"
        );
    }

    private void prepareBulgarianEmail() {

        prepareCommonReadyDocument();

        when(
                request.language()
        ).thenReturn(
                "bg"
        );
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

        when(
                documentInsightsPdfService.generate(
                        document,
                        request,
                        currentUser
                )
        ).thenReturn(
                PDF_CONTENT
        );
    }
}