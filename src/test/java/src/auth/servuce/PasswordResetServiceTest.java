package src.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import src.auth.entity.PasswordResetToken;
import src.auth.repository.PasswordResetTokenRepository;
import src.auth.repository.UserRepository;
import src.common.exception.ApiErrorCodes;
import src.common.exception.BadRequestException;
import src.common.exception.EmailDeliveryException;
import src.entity.User;
import src.mail.service.MailService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository
            userRepository;

    @Mock
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Mock
    private PasswordEncoder
            passwordEncoder;

    @Mock
    private MailService
            mailService;

    @Mock
    private RefreshTokenService
            refreshTokenService;

    @Mock
    private User
            user;

    private PasswordResetService
            passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService =
                new PasswordResetService(
                        userRepository,
                        passwordResetTokenRepository,
                        passwordEncoder,
                        mailService,
                        refreshTokenService
                );

        ReflectionTestUtils.setField(
                passwordResetService,
                "frontendBaseUrl",
                "http://localhost:5173"
        );

        ReflectionTestUtils.setField(
                passwordResetService,
                "expirationMinutes",
                30L
        );
    }

    @Nested
    class RequestPasswordReset {

        @Test
        void shouldNormalizeEmailBeforeSearchingForUser() {
            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.empty()
            );

            passwordResetService
                    .requestPasswordReset(
                            "  USER@EXAMPLE.COM  "
                    );

            verify(
                    userRepository
            ).findByEmail(
                    "user@example.com"
            );
        }

        @Test
        void shouldReturnNormallyWhenEmailDoesNotExist() {
            when(
                    userRepository.findByEmail(
                            "missing@example.com"
                    )
            ).thenReturn(
                    Optional.empty()
            );

            assertDoesNotThrow(
                    () ->
                            passwordResetService
                                    .requestPasswordReset(
                                            "missing@example.com"
                                    )
            );

            verifyNoInteractions(
                    passwordResetTokenRepository
            );

            verifyNoInteractions(
                    mailService
            );
        }

        @Test
        void shouldHandleNullEmailWithoutRevealingAccountInformation() {
            when(
                    userRepository.findByEmail(
                            ""
                    )
            ).thenReturn(
                    Optional.empty()
            );

            assertDoesNotThrow(
                    () ->
                            passwordResetService
                                    .requestPasswordReset(
                                            null
                                    )
            );

            verify(
                    userRepository
            ).findByEmail(
                    ""
            );

            verifyNoInteractions(
                    passwordResetTokenRepository
            );

            verifyNoInteractions(
                    mailService
            );
        }

        @Test
        void shouldDeletePreviousTokensBeforeCreatingNewToken() {
            prepareUserForResetRequest();

            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );

            passwordResetService
                    .requestPasswordReset(
                            "user@example.com"
                    );

            verify(
                    passwordResetTokenRepository
            ).deleteAllByUser(
                    user
            );

            verify(
                    passwordResetTokenRepository
            ).save(
                    any(
                            PasswordResetToken.class
                    )
            );
        }

        @Test
        void shouldStoreHashedTokenAndSendRawTokenOnlyInEmailUrl()
                throws Exception {

            prepareUserForResetRequest();

            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );

            ArgumentCaptor<PasswordResetToken>
                    tokenCaptor =
                    ArgumentCaptor.forClass(
                            PasswordResetToken.class
                    );

            ArgumentCaptor<String>
                    bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            passwordResetService
                    .requestPasswordReset(
                            "user@example.com"
                    );

            verify(
                    passwordResetTokenRepository
            ).save(
                    tokenCaptor.capture()
            );

            verify(
                    mailService
            ).sendTextEmail(
                    eq(
                            "user@example.com"
                    ),
                    eq(
                            "Reset your OurVault password"
                    ),
                    bodyCaptor.capture()
            );

            PasswordResetToken savedToken =
                    tokenCaptor.getValue();

            String emailBody =
                    bodyCaptor.getValue();

            assertSame(
                    user,
                    savedToken.getUser()
            );

            assertNotNull(
                    savedToken.getTokenHash()
            );

            assertFalse(
                    savedToken
                            .getTokenHash()
                            .isBlank()
            );

            assertNotNull(
                    savedToken.getExpiresAt()
            );

            String prefix =
                    "http://localhost:5173/reset-password?token=";

            int start =
                    emailBody.indexOf(
                            prefix
                    );

            assertTrue(
                    start >= 0,
                    "Email should contain the reset URL"
            );

            start +=
                    prefix.length();

            int end =
                    emailBody.indexOf(
                            '\n',
                            start
                    );

            if (end == -1) {
                end =
                        emailBody.length();
            }

            String rawToken =
                    emailBody
                            .substring(
                                    start,
                                    end
                            )
                            .trim();

            assertFalse(
                    rawToken.isBlank()
            );

            /*
             * Raw token must never be stored directly.
             */
            assertNotEquals(
                    rawToken,
                    savedToken.getTokenHash()
            );

            /*
             * Stored value must be SHA-256(raw token).
             */
            assertEquals(
                    sha256Hex(
                            rawToken
                    ),
                    savedToken.getTokenHash()
            );
        }

        @Test
        void shouldCreateTokenWithExpectedExpirationTime() {
            prepareUserForResetRequest();

            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );

            LocalDateTime before =
                    LocalDateTime.now();

            ArgumentCaptor<PasswordResetToken>
                    tokenCaptor =
                    ArgumentCaptor.forClass(
                            PasswordResetToken.class
                    );

            passwordResetService
                    .requestPasswordReset(
                            "user@example.com"
                    );

            LocalDateTime after =
                    LocalDateTime.now();

            verify(
                    passwordResetTokenRepository
            ).save(
                    tokenCaptor.capture()
            );

            LocalDateTime expiration =
                    tokenCaptor
                            .getValue()
                            .getExpiresAt();

            assertFalse(
                    expiration.isBefore(
                            before.plusMinutes(
                                    30
                            )
                    )
            );

            assertFalse(
                    expiration.isAfter(
                            after.plusMinutes(
                                    30
                            )
                    )
            );
        }

        @Test
        void shouldUseFirstNameInEmailWhenAvailable() {
            when(
                    user.getFirstName()
            ).thenReturn(
                    "Svetlin"
            );

            when(
                    user.getEmail()
            ).thenReturn(
                    "user@example.com"
            );

            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );

            ArgumentCaptor<String>
                    bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            passwordResetService
                    .requestPasswordReset(
                            "user@example.com"
                    );

            verify(
                    mailService
            ).sendTextEmail(
                    eq(
                            "user@example.com"
                    ),
                    eq(
                            "Reset your OurVault password"
                    ),
                    bodyCaptor.capture()
            );

            assertTrue(
                    bodyCaptor
                            .getValue()
                            .contains(
                                    "Hello Svetlin,"
                            )
            );
        }

        @Test
        void shouldFallbackToUsernameWhenFirstNameMissing() {
            when(
                    user.getFirstName()
            ).thenReturn(
                    null
            );

            when(
                    user.getUsername()
            ).thenReturn(
                    "svetlin"
            );

            when(
                    user.getEmail()
            ).thenReturn(
                    "user@example.com"
            );

            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );

            ArgumentCaptor<String>
                    bodyCaptor =
                    ArgumentCaptor.forClass(
                            String.class
                    );

            passwordResetService
                    .requestPasswordReset(
                            "user@example.com"
                    );

            verify(
                    mailService
            ).sendTextEmail(
                    eq(
                            "user@example.com"
                    ),
                    eq(
                            "Reset your OurVault password"
                    ),
                    bodyCaptor.capture()
            );

            assertTrue(
                    bodyCaptor
                            .getValue()
                            .contains(
                                    "Hello svetlin,"
                            )
            );
        }

        @Test
        void shouldNotExposeEmailDeliveryFailure() {
            prepareUserForResetRequest();

            when(
                    userRepository.findByEmail(
                            "user@example.com"
                    )
            ).thenReturn(
                    Optional.of(
                            user
                    )
            );

            doThrow(
                    new EmailDeliveryException(
                            "Email could not be sent",
                            new RuntimeException(
                                    "SMTP failure"
                            )
                    )
            ).when(
                    mailService
            ).sendTextEmail(
                    anyString(),
                    anyString(),
                    anyString()
            );

            assertDoesNotThrow(
                    () ->
                            passwordResetService
                                    .requestPasswordReset(
                                            "user@example.com"
                                    )
            );

            verify(
                    passwordResetTokenRepository
            ).save(
                    any(
                            PasswordResetToken.class
                    )
            );
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void shouldRejectNullToken() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            null,
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordResetTokenRepository
            );
        }

        @Test
        void shouldRejectBlankToken() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            "   ",
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordResetTokenRepository
            );
        }

        @Test
        void shouldRejectUnknownToken()
                throws Exception {

            String rawToken =
                    "unknown-token";

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            rawToken,
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordEncoder
            );

            verifyNoInteractions(
                    refreshTokenService
            );
        }

        @Test
        void shouldTrimRawTokenBeforeHashing()
                throws Exception {

            String rawToken =
                    "reset-token";

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            "   reset-token   ",
                                            "new-password"
                                    )
            );

            verify(
                    passwordResetTokenRepository
            ).findByTokenHash(
                    sha256Hex(
                            rawToken
                    )
            );
        }

        @Test
        void shouldRejectAlreadyUsedToken()
                throws Exception {

            String rawToken =
                    "used-token";

            PasswordResetToken resetToken =
                    mock(
                            PasswordResetToken.class
                    );

            when(
                    resetToken.getUsedAt()
            ).thenReturn(
                    LocalDateTime.now()
            );

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            rawToken,
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordEncoder
            );

            verifyNoInteractions(
                    refreshTokenService
            );
        }

        @Test
        void shouldRejectExpiredToken()
                throws Exception {

            String rawToken =
                    "expired-token";

            PasswordResetToken resetToken =
                    mock(
                            PasswordResetToken.class
                    );

            when(
                    resetToken.getUsedAt()
            ).thenReturn(
                    null
            );

            when(
                    resetToken.getExpiresAt()
            ).thenReturn(
                    LocalDateTime
                            .now()
                            .minusMinutes(
                                    1
                            )
            );

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            rawToken,
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordEncoder
            );

            verifyNoInteractions(
                    refreshTokenService
            );
        }

        @Test
        void shouldRejectTokenWithoutExpiration()
                throws Exception {

            String rawToken =
                    "token-without-expiration";

            PasswordResetToken resetToken =
                    mock(
                            PasswordResetToken.class
                    );

            when(
                    resetToken.getUsedAt()
            ).thenReturn(
                    null
            );

            when(
                    resetToken.getExpiresAt()
            ).thenReturn(
                    null
            );

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            rawToken,
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordEncoder
            );

            verifyNoInteractions(
                    refreshTokenService
            );
        }

        @Test
        void shouldRejectTokenWithoutUser()
                throws Exception {

            String rawToken =
                    "token-without-user";

            PasswordResetToken resetToken =
                    mock(
                            PasswordResetToken.class
                    );

            when(
                    resetToken.getUsedAt()
            ).thenReturn(
                    null
            );

            when(
                    resetToken.getExpiresAt()
            ).thenReturn(
                    LocalDateTime
                            .now()
                            .plusMinutes(
                                    10
                            )
            );

            when(
                    resetToken.getUser()
            ).thenReturn(
                    null
            );

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            rawToken,
                                            "new-password"
                                    )
            );

            verifyNoInteractions(
                    passwordEncoder
            );

            verifyNoInteractions(
                    refreshTokenService
            );
        }

        @Test
        void shouldResetPasswordAndRevokeRefreshTokens()
                throws Exception {

            String rawToken =
                    "valid-reset-token";

            String newPassword =
                    "new-secure-password";

            String encodedPassword =
                    "encoded-password";

            PasswordResetToken resetToken =
                    mock(
                            PasswordResetToken.class
                    );

            UUID userId =
                    UUID.randomUUID();

            LocalDateTime expiration =
                    LocalDateTime
                            .now()
                            .plusMinutes(
                                    15
                            );

            when(
                    resetToken.getUsedAt()
            ).thenReturn(
                    null
            );

            when(
                    resetToken.getExpiresAt()
            ).thenReturn(
                    expiration
            );

            when(
                    resetToken.getUser()
            ).thenReturn(
                    user
            );

            when(
                    user.getId()
            ).thenReturn(
                    userId
            );

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );

            when(
                    passwordEncoder.encode(
                            newPassword
                    )
            ).thenReturn(
                    encodedPassword
            );

            LocalDateTime before =
                    LocalDateTime.now();

            passwordResetService
                    .resetPassword(
                            rawToken,
                            newPassword
                    );

            LocalDateTime after =
                    LocalDateTime.now();

            verify(
                    user
            ).setPasswordHash(
                    encodedPassword
            );

            ArgumentCaptor<LocalDateTime>
                    usedAtCaptor =
                    ArgumentCaptor.forClass(
                            LocalDateTime.class
                    );

            verify(
                    resetToken
            ).setUsedAt(
                    usedAtCaptor.capture()
            );

            LocalDateTime usedAt =
                    usedAtCaptor.getValue();

            assertFalse(
                    usedAt.isBefore(
                            before
                    )
            );

            assertFalse(
                    usedAt.isAfter(
                            after
                    )
            );

            verify(
                    userRepository
            ).save(
                    user
            );

            verify(
                    passwordResetTokenRepository
            ).save(
                    resetToken
            );

            verify(
                    refreshTokenService
            ).revokeAllRefreshTokens(
                    user
            );
        }

        @Test
        void shouldNotRevokeRefreshTokensWhenPasswordEncodingFails()
                throws Exception {

            String rawToken =
                    "valid-reset-token";

            PasswordResetToken resetToken =
                    mock(
                            PasswordResetToken.class
                    );

            when(
                    resetToken.getUsedAt()
            ).thenReturn(
                    null
            );

            when(
                    resetToken.getExpiresAt()
            ).thenReturn(
                    LocalDateTime
                            .now()
                            .plusMinutes(
                                    10
                            )
            );

            when(
                    resetToken.getUser()
            ).thenReturn(
                    user
            );

            when(
                    passwordResetTokenRepository
                            .findByTokenHash(
                                    sha256Hex(
                                            rawToken
                                    )
                            )
            ).thenReturn(
                    Optional.of(
                            resetToken
                    )
            );

            when(
                    passwordEncoder.encode(
                            "new-password"
                    )
            ).thenThrow(
                    new IllegalArgumentException(
                            "Encoding failed"
                    )
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            passwordResetService
                                    .resetPassword(
                                            rawToken,
                                            "new-password"
                                    )
            );

            verify(
                    userRepository,
                    never()
            ).save(
                    any(
                            User.class
                    )
            );

            verify(
                    refreshTokenService,
                    never()
            ).revokeAllRefreshTokens(
                    any(
                            User.class
                    )
            );
        }
    }

    private void prepareUserForResetRequest() {
        when(
                user.getEmail()
        ).thenReturn(
                "user@example.com"
        );

        when(
                user.getFirstName()
        ).thenReturn(
                "Svetlin"
        );
    }

    private String sha256Hex(
            String value
    ) throws Exception {
        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] hash =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        return HexFormat
                .of()
                .formatHex(
                        hash
                );
    }
}