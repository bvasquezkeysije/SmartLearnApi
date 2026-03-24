package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.user.UserCreateRequest;
import com.bardales.SmartLearnApi.dto.user.UserPasswordUpdateRequest;
import com.bardales.SmartLearnApi.dto.user.UserPasswordUpdateResponse;
import com.bardales.SmartLearnApi.dto.user.UserResponse;
import com.bardales.SmartLearnApi.dto.user.UserUpdateRequest;
import com.bardales.SmartLearnApi.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam Long requesterUserId) {
        return userService.listUsers(requesterUserId);
    }

    @PostMapping
    public UserResponse create(@RequestParam Long requesterUserId, @Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(requesterUserId, request);
    }

    @PutMapping("/{userId}")
    public UserResponse update(
            @PathVariable Long userId,
            @RequestParam Long requesterUserId,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(requesterUserId, userId, request);
    }

    @PatchMapping("/{userId}/password")
    public UserPasswordUpdateResponse updatePassword(
            @PathVariable Long userId,
            @RequestParam Long requesterUserId,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        return userService.updateUserPassword(requesterUserId, userId, request);
    }

    @PatchMapping("/{userId}/activate")
    public UserResponse activate(@PathVariable Long userId, @RequestParam Long requesterUserId) {
        return userService.activateUser(requesterUserId, userId);
    }

    @PatchMapping("/{userId}/deactivate")
    public UserResponse deactivate(@PathVariable Long userId, @RequestParam Long requesterUserId) {
        return userService.deactivateUser(requesterUserId, userId);
    }
}
