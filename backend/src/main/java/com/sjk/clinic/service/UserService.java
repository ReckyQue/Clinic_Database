package com.sjk.clinic.service;

import com.sjk.clinic.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    Optional<User> findByUsername(String username);
    User createUser(User user);
    void initializeDefaultAdmin();
    Page<User> getUsers(String username, String realName, String role, String status, Pageable pageable);
    Optional<User> getUserById(Long id);
    User updateUser(User user);
    void deleteUser(Long id);
    void updateUserStatus(Long id, User.UserStatus status);
    List<User> getAllUsers();
}
