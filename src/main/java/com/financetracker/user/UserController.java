package com.financetracker.user;

import com.financetracker.security.AuthPrincipal;
import com.financetracker.user.dto.UserResponse;
import com.financetracker.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "The authenticated user's profile")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.getCurrentUser(principal.id());
    }
}
