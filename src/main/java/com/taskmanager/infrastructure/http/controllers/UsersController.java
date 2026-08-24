package com.taskmanager.infrastructure.http.controllers;

import com.taskmanager.application.AuthenticateUserUseCase;
import com.taskmanager.application.DeleteAccountUseCase;
import com.taskmanager.application.RegisterUserUseCase;
import com.taskmanager.application.UpdateUserProfileUseCase;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.SessionRepository;
import com.taskmanager.infrastructure.http.AuthenticatedUser;
import com.taskmanager.infrastructure.http.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final SessionRepository sessionRepository;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;

    public UsersController(RegisterUserUseCase registerUserUseCase,
                           AuthenticateUserUseCase authenticateUserUseCase,
                           SessionRepository sessionRepository, DeleteAccountUseCase deleteAccountUseCase, UpdateUserProfileUseCase updateUserProfileUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.sessionRepository = sessionRepository;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@RequestBody RegisterUserRequest request) {
        User user = registerUserUseCase.execute(request.email, request.username, request.password, request.displayName);
        var response = new RegisterUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName());
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserResponse> login(@RequestBody LoginUserRequest request) {
        AuthenticateUserUseCase.Session session =
                authenticateUserUseCase.execute(request.identifier, request.password);

        var response = new LoginUserResponse(
                session.getUser().getId(),
                session.getUser().getUsername(),
                session.getToken()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            sessionRepository.delete(token);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticatedUser User user,
                                              @RequestHeader("Authorization") String authHeader) {
        deleteAccountUseCase.execute(user.getId());

        String token = authHeader.substring("Bearer ".length()).trim();
        sessionRepository.delete(token);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticatedUser User user) {

        var response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName()
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticatedUser User user,
                                                      @RequestBody UpdateUserProfileRequest request) {
        User updated = updateUserProfileUseCase.execute(user.getId(), request.username, request.email, request.displayName);
        return ResponseEntity.ok(new UserResponse(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getDisplayName()));
    }
}