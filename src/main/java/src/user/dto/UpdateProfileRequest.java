package src.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(

        @NotBlank(message = "Username is required")
        @Size(
                min = 3,
                max = 50,
                message = "Username must be between 3 and 50 characters"
        )
        String username,

        @NotBlank(message = "First name is required")
        @Size(
                max = 100,
                message = "First name cannot exceed 100 characters"
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(
                max = 100,
                message = "Last name cannot exceed 100 characters"
        )
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email address is invalid")
        @Size(
                max = 255,
                message = "Email cannot exceed 255 characters"
        )
        String email,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth
) {
}