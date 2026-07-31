package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.dto.UserDto;
import com.sjk.clinic.entity.User;
import com.sjk.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<User> userPage = userService.getUsers(username, realName, role, status, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("records", userPage.getContent().stream().map(UserDto::fromUser).toList());
        result.put("total", userPage.getTotalElements());
        result.put("current", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return Result.success(UserDto.fromUser(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> createUser(@RequestBody User user) {
        User savedUser = userService.createUser(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", savedUser.getId());

        return Result.success("用户创建成功", result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success("用户更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        userService.updateUserStatus(id, User.UserStatus.valueOf(status));
        return Result.success("用户状态更新成功", null);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream().map(UserDto::fromUser).toList();
        return Result.success(users);
    }
}
