package src.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import src.auth.dto.*;
import src.auth.service.AuthService;
import src.auth.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final PasswordResetService
            passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody
            RegisterRequest request
    ) {
        return ResponseEntity.ok(
                authService.register(
                        request
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody
            LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(
                        request
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody
            RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
                authService.refresh(
                        request
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody
            RefreshTokenRequest request
    ) {
        authService.logout(
                request
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid
            @RequestBody
            ForgotPasswordRequest request
    ) {
        passwordResetService
                .requestPasswordReset(
                        request.email()
                );

        /*
         * Always the same response regardless of whether
         * the email exists.
         */
        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {
        passwordResetService
                .resetPassword(
                        request.token(),
                        request.newPassword()
                );

        return ResponseEntity
                .noContent()
                .build();
    }
}