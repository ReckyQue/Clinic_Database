package com.sjk.clinic.service.impl;

import com.sjk.clinic.entity.User;
import com.sjk.clinic.repository.UserRepository;
import com.sjk.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }
    
    @Override
    public void initializeDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRealName("管理员");
            admin.setRole(User.UserRole.ADMIN);
            admin.setStatus(User.UserStatus.ACTIVE);
            createUser(admin);
        }
        if (!userRepository.existsByUsername("member")) {
            User member = new User();
            member.setUsername("member");
            member.setPassword("member123");
            member.setRealName("成员");
            member.setRole(User.UserRole.MEMBER);
            member.setStatus(User.UserStatus.ACTIVE);
            createUser(member);
        }
        if (!userRepository.existsByUsername("guest")) {
            User guest = new User();
            guest.setUsername("guest");
            guest.setPassword("guest123");
            guest.setRealName("游客");
            guest.setRole(User.UserRole.GUEST);
            guest.setStatus(User.UserStatus.ACTIVE);
            createUser(guest);
        }
    }
    
    @Override
    public Page<User> getUsers(String username, String realName, String role, String status, Pageable pageable) {
        User.UserRole roleFilter = parseRole(role);
        User.UserStatus statusFilter = parseStatus(status);
        return userRepository.findBySearchCriteria(username, realName, roleFilter, statusFilter, pageable);
    }

    private User.UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return User.UserRole.valueOf(role);
    }

    private User.UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return User.UserStatus.valueOf(status);
    }
    
    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        existingUser.setRealName(user.getRealName());
        existingUser.setRole(user.getRole());
        existingUser.setPhone(user.getPhone());
        existingUser.setEmail(user.getEmail());
        
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return userRepository.save(existingUser);
    }
    
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("不能删除管理员账户");
        }
        
        userRepository.deleteById(id);
    }
    
    @Override
    public void updateUserStatus(Long id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if ("admin".equals(user.getUsername()) && status == User.UserStatus.INACTIVE) {
            throw new RuntimeException("不能禁用管理员账户");
        }
        
        user.setStatus(status);
        userRepository.save(user);
    }
    
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
