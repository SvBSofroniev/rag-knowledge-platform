package src.user.dto;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String email,
        String role
) {
}
