package src.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.auth.repository.UserRepository;
import src.common.exception.BadRequestException;
import src.common.exception.ConflictException;
import src.common.exception.ResourceNotFoundException;
import src.entity.User;
import src.user.dto.CurrentUserResponse;
import src.user.dto.UpdateProfileRequest;
import src.user.dto.UserSearchResponse;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int SEARCH_LIMIT = 10;

    private final UserRepository userRepository;

    @Transactional
    public CurrentUserResponse updateProfile(
            UpdateProfileRequest request,
            User currentUser
    ) {
        User user = userRepository
                .findById(currentUser.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        String normalizedUsername =
                request.username().trim();

        String normalizedEmail =
                request.email()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        String normalizedFirstName =
                request.firstName().trim();

        String normalizedLastName =
                request.lastName().trim();

        if (userRepository.existsByEmailAndIdNot(
                normalizedEmail,
                user.getId()
        )) {
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        if (userRepository.existsByUsernameAndIdNot(
                normalizedUsername,
                user.getId()
        )) {
            throw new ConflictException(
                    "An account with this username already exists"
            );
        }

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

        User savedUser =
                userRepository.save(user);

        return toCurrentUserResponse(
                savedUser
        );
    }

    public List<UserSearchResponse> searchUsers(
            String query,
            User currentUser
    ) {
        if (query == null ||
                query.trim().length() < 2) {

            throw new BadRequestException(
                    "Search query must contain at least 2 characters"
            );
        }

        return userRepository
                .searchUsers(
                        query.trim(),
                        currentUser.getId(),
                        PageRequest.of(
                                0,
                                SEARCH_LIMIT
                        )
                )
                .stream()
                .map(user ->
                        new UserSearchResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail()
                        )
                )
                .toList();
    }

    private CurrentUserResponse toCurrentUserResponse(
            User user
    ) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDateOfBirth(),
                user.getRole()
        );
    }
}