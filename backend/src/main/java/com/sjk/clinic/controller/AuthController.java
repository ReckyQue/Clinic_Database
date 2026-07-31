package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.dto.LoginRequest;
import com.sjk.clinic.dto.LoginResponse;
import com.sjk.clinic.entity.User;
import com.sjk.clinic.service.UserService;
import com.sjk.clinic.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            User existingUser = userService.findByUsername(request.getUsername()).orElse(null);
            if (existingUser == null) {
                return Result.error(401, "用户不存在");
            }
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            
            if (user == null) {
                return Result.error(401, "用户不存在");
            }
            
            String token = jwtUtil.generateToken(userDetails);
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(LoginResponse.UserDto.fromUser(user));
            
            return Result.success("登录成功", response);
        } catch (AuthenticationException e) {
            return Result.error(401, "用户名或密码错误");
        }
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("登出成功", null);
    }
    
    @GetMapping("/profile")
    public Result<LoginResponse.UserDto> getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isBlank() || !token.startsWith("Bearer ")) {
            return Result.error(401, "缺少认证信息，请先登录");
        }
        try {
            String jwt = token.substring(7).trim();
            if (jwt.isEmpty()) {
                return Result.error(401, "缺少认证信息，请先登录");
            }
            String username = jwtUtil.extractUsername(jwt);
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                return Result.error(401, "用户不存在");
            }
            
            return Result.success(LoginResponse.UserDto.fromUser(user));
        } catch (JwtException | IllegalArgumentException e) {
            return Result.error(401, "登录已过期或无效，请重新登录");
        }
    }
}
