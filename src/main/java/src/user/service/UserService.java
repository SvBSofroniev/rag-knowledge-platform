package src.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import src.auth.repository.UserRepository;
import src.common.exception.BadRequestException;
import src.entity.User;

import src.user.dto.UserSearchResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int SEARCH_LIMIT = 10;

    private final UserRepository userRepository;

    public List<UserSearchResponse> searchUsers(
            String query,
            User currentUser
    ) {
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException(
                    "Search query must contain at least 2 characters"
            );
        }

        return userRepository.searchUsers(
                        query.trim(),
                        currentUser.getId(),
                        PageRequest.of(0, SEARCH_LIMIT)
                )
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                ))
                .toList();
    }
}