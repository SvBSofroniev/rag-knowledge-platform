package src.user.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String role
) {
}