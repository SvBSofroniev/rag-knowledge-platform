package src.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static src.common.validation.ValidationErrorCodes.*;

public record RegisterRequest(

        @NotBlank(
                message = USERNAME_REQUIRED
        )
        @Size(
                min = 3,
                max = 50,
                message = USERNAME_LENGTH
        )
        String username,

        @NotBlank(
                message = FIRST_NAME_REQUIRED
        )
        @Size(
                max = 100,
                message = FIRST_NAME_LENGTH
        )
        String firstName,

        @NotBlank(
                message = LAST_NAME_REQUIRED
        )
        @Size(
                max = 100,
                message = LAST_NAME_LENGTH
        )
        String lastName,

        @NotBlank(
                message = EMAIL_REQUIRED
        )
        @Email(
                message = EMAIL_INVALID
        )
        @Size(
                max = 255,
                message = EMAIL_LENGTH
        )
        String email,

        @Past(
                message = DATE_OF_BIRTH_PAST
        )
        LocalDate dateOfBirth,

        @NotBlank(
                message = PASSWORD_REQUIRED
        )
        @Size(
                min = 8,
                max = 100,
                message = PASSWORD_LENGTH
        )
        String password
) {
}