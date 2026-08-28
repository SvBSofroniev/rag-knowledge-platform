package src.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.entity.User;
import src.user.dto.CurrentUserResponse;
import src.user.dto.UpdateProfileRequest;
import src.user.dto.UserSearchResponse;
import src.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal User currentUser
    ) {
        return new CurrentUserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getDateOfBirth(),
                currentUser.getRole()
        );
    }

    @PatchMapping("/me")
    public CurrentUserResponse updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return userService.updateProfile(
                request,
                currentUser
        );
    }

    @GetMapping("/search")
    public List<UserSearchResponse> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal User currentUser
    ) {
        return userService.searchUsers(
                query,
                currentUser
        );
    }
}