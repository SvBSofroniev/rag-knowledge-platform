package src.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.auth.entity.RefreshToken;
import src.auth.repository.RefreshTokenRepository;
import src.common.exception.ApiErrorCodes;
import src.common.exception.UnauthorizedException;
import src.entity.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_DAYS = 7;
    private static final int TOKEN_SIZE_BYTES = 64;

    private final RefreshTokenRepository
            refreshTokenRepository;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Transactional
    public String createRefreshToken(
            User user
    ) {
        String rawToken =
                generateRandomToken();

        String tokenHash =
                hashToken(rawToken);

        RefreshToken refreshToken =
                new RefreshToken();

        refreshToken.setUser(
                user
        );

        refreshToken.setTokenHash(
                tokenHash
        );

        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusDays(
                                REFRESH_TOKEN_DAYS
                        )
        );

        refreshToken.setRevoked(
                false
        );

        refreshTokenRepository.save(
                refreshToken
        );

        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(
            String rawToken
    ) {
        if (rawToken == null ||
                rawToken.isBlank()) {

            throw new UnauthorizedException(
                    ApiErrorCodes.INVALID_REFRESH_TOKEN,
                    "Invalid refresh token"
            );
        }

        String tokenHash =
                hashToken(
                        rawToken
                );

        RefreshToken token =
                refreshTokenRepository
                        .findByTokenHash(
                                tokenHash
                        )
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        ApiErrorCodes.INVALID_REFRESH_TOKEN,
                                        "Invalid refresh token"
                                )
                        );

        if (token.isRevoked()) {
            throw new UnauthorizedException(
                    ApiErrorCodes.REFRESH_TOKEN_REVOKED,
                    "Refresh token has been revoked"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (token.getExpiresAt() == null ||
                !token.getExpiresAt()
                        .isAfter(now)) {

            throw new UnauthorizedException(
                    ApiErrorCodes.REFRESH_TOKEN_EXPIRED,
                    "Refresh token has expired"
            );
        }

        User user =
                token.getUser();

        if (user == null) {
            throw new UnauthorizedException(
                    ApiErrorCodes.INVALID_REFRESH_TOKEN,
                    "Invalid refresh token"
            );
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException(
                    ApiErrorCodes.ACCOUNT_DISABLED,
                    "Account is disabled"
            );
        }

        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException(
                    ApiErrorCodes.ACCOUNT_LOCKED,
                    "Account is locked"
            );
        }

        return token;
    }

    /*
     * Logout is intentionally idempotent.
     */
    @Transactional
    public void revokeRefreshToken(
            String rawToken
    ) {
        if (rawToken == null ||
                rawToken.isBlank()) {

            return;
        }

        String tokenHash =
                hashToken(
                        rawToken
                );

        refreshTokenRepository
                .findByTokenHash(
                        tokenHash
                )
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevoked(
                                true
                        );

                        refreshTokenRepository
                                .save(token);
                    }
                });
    }

    private String generateRandomToken() {
        byte[] bytes =
                new byte[
                        TOKEN_SIZE_BYTES
                        ];

        secureRandom.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }

    private String hashToken(
            String token
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64
                    .getEncoder()
                    .encodeToString(
                            hash
                    );

        } catch (
                NoSuchAlgorithmException exception
        ) {
            throw new IllegalStateException(
                    "SHA-256 hashing is unavailable",
                    exception
            );
        }
    }

    @Transactional
    public void revokeAllRefreshTokens(
            User user
    ) {
        if (user == null ||
                user.getId() == null) {

            throw new IllegalArgumentException(
                    "User is required"
            );
        }

        refreshTokenRepository
                .revokeAllByUserId(
                        user.getId()
                );
    }
}