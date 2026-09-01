package src.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom
            SECURE_RANDOM =
            new SecureRandom();

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository
            passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;

    @Value("${ourvault.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${ourvault.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    /*
     * IMPORTANT:
     *
     * This method intentionally returns normally even when
     * no account exists for the supplied email.
     *
     * The API therefore does not reveal which email
     * addresses are registered.
     */
    @Transactional
    public void requestPasswordReset(
            String email
    ) {
        String normalizedEmail =
                normalizeEmail(
                        email
                );

        userRepository
                .findByEmail(
                        normalizedEmail
                )
                .ifPresent(
                        this::createAndSendResetToken
                );
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {
        if (rawToken == null ||
                rawToken.isBlank()) {

            throw invalidToken();
        }

        String tokenHash =
                hashToken(
                        rawToken.trim()
                );

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHash(
                                tokenHash
                        )
                        .orElseThrow(
                                this::invalidToken
                        );

        if (resetToken.getUsedAt() != null) {
            throw invalidToken();
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (resetToken
                .getExpiresAt()
                .isBefore(now)) {

            throw new BadRequestException(
                    ApiErrorCodes.PASSWORD_RESET_TOKEN_EXPIRED,
                    "Password reset token has expired"
            );
        }

        User user =
                resetToken.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(
                        newPassword
                )
        );

        resetToken.setUsedAt(
                now
        );

        userRepository.save(
                user
        );

        passwordResetTokenRepository.save(
                resetToken
        );

        log.info(
                "Password reset completed for user {}",
                user.getId()
        );
    }

    private void createAndSendResetToken(
            User user
    ) {
        /*
         * Only the newest reset link remains valid.
         */
        passwordResetTokenRepository
                .deleteAllByUser(
                        user
                );

        String rawToken =
                generateSecureToken();

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setUser(
                user
        );

        resetToken.setTokenHash(
                hashToken(
                        rawToken
                )
        );

        resetToken.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(
                                expirationMinutes
                        )
        );

        passwordResetTokenRepository.save(
                resetToken
        );

        String resetUrl =
                frontendBaseUrl +
                        "/reset-password?token=" +
                        rawToken;

        String body = """
                Hello %s,

                A password reset was requested for your OurVault account.

                Use the following link to choose a new password:

                %s

                This link expires in %d minutes and can only be used once.

                If you did not request this password reset, you can ignore this email.

                OurVault
                """.formatted(
                user.getFirstName() == null ||
                        user.getFirstName().isBlank()
                        ? user.getUsername()
                        : user.getFirstName(),
                resetUrl,
                expirationMinutes
        );

        try {
            mailService.sendTextEmail(
                    user.getEmail(),
                    "Reset your OurVault password",
                    body
            );

        } catch (EmailDeliveryException exception) {
            /*
             * Do not expose whether the supplied email belongs
             * to a registered account.
             *
             * The failure remains visible in server logs.
             */
            log.error(
                    "Password reset email could not be sent for user {}",
                    user.getId(),
                    exception
            );
        }
    }

    private String normalizeEmail(
            String email
    ) {
        if (email == null) {
            return "";
        }

        return email
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String generateSecureToken() {
        byte[] randomBytes =
                new byte[32];

        SECURE_RANDOM.nextBytes(
                randomBytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        randomBytes
                );
    }

    private String hashToken(
            String rawToken
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(
                            hash
                    );

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private BadRequestException invalidToken() {
        return new BadRequestException(
                ApiErrorCodes.PASSWORD_RESET_TOKEN_INVALID,
                "Password reset token is invalid"
        );
    }
}