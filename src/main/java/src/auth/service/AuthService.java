package src.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import src.auth.dto.AuthResponse;
import src.auth.dto.LoginRequest;
import src.auth.dto.RefreshTokenRequest;
import src.auth.dto.RegisterRequest;
import src.auth.entity.RefreshToken;
import src.auth.jwt.JwtService;
import src.auth.repository.UserRepository;
import src.common.exception.ConflictException;
import src.common.exception.UnauthorizedException;
import src.entity.User;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {

        String normalizedEmail =
                request.email()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        String normalizedUsername =
                request.username()
                        .trim();

        String normalizedFirstName =
                request.firstName()
                        .trim();

        String normalizedLastName =
                request.lastName()
                        .trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ConflictException(
                    "An account with this username already exists"
            );
        }

        User user = new User();

        user.setUsername(
                normalizedUsername
        );

        user.setEmail(
                normalizedEmail
        );

        user.setFirstName(
                normalizedFirstName
        );

        user.setLastName(
                normalizedLastName
        );

        user.setDateOfBirth(
                request.dateOfBirth()
        );

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setRole("USER");
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        User savedUser =
                userRepository.save(user);

        String accessToken =
                jwtService.generateToken(
                        savedUser
                );

        String refreshToken =
                refreshTokenService.createRefreshToken(
                        savedUser
                );

        return buildAuthResponse(
                savedUser,
                accessToken,
                refreshToken
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {
        String normalizedEmail =
                request.email()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        "Invalid email or password"
                                )
                        );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException(
                    "Account is disabled"
            );
        }

        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException(
                    "Account is locked"
            );
        }

        String accessToken =
                jwtService.generateToken(user);

        String refreshToken =
                refreshTokenService.createRefreshToken(
                        user
                );

        return buildAuthResponse(
                user,
                accessToken,
                refreshToken
        );
    }

    public AuthResponse refresh(
            RefreshTokenRequest request
    ) {
        RefreshToken currentToken =
                refreshTokenService
                        .validateRefreshToken(
                                request.refreshToken()
                        );

        User user =
                currentToken.getUser();

        refreshTokenService.revokeRefreshToken(
                request.refreshToken()
        );

        String accessToken =
                jwtService.generateToken(user);

        String newRefreshToken =
                refreshTokenService
                        .createRefreshToken(user);

        return buildAuthResponse(
                user,
                accessToken,
                newRefreshToken
        );
    }

    public void logout(
            RefreshTokenRequest request
    ) {
        refreshTokenService.revokeRefreshToken(
                request.refreshToken()
        );
    }

    private AuthResponse buildAuthResponse(
            User user,
            String accessToken,
            String refreshToken
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}