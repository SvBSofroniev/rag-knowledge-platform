package src.user.dto;

import java.util.UUID;

public record UserSearchResponse(
        UUID id,
        String username,
        String email
) {
}
