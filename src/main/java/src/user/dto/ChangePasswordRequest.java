package src.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static src.common.validation.ValidationErrorCodes.*;

public record ChangePasswordRequest(

        @NotBlank(
                message = CURRENT_PASSWORD_REQUIRED
        )
        String currentPassword,

        @NotBlank(
                message = NEW_PASSWORD_REQUIRED
        )
        @Size(
                min = 8,
                max = 100,
                message = NEW_PASSWORD_LENGTH
        )
        String newPassword,

        @NotBlank(
                message = PASSWORD_CONFIRMATION_REQUIRED
        )
        String confirmNewPassword
) {
}