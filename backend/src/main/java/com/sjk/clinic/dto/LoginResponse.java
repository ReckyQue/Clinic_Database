package com.sjk.clinic.dto;

import com.sjk.clinic.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserDto user;
    
    @Data
    public static class UserDto {
        private Long id;
        private String username;
        private String realName;
        private String role;
        private String phone;
        private String email;
        
        public static UserDto fromUser(User user) {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRealName(user.getRealName());
            dto.setRole(user.getRole() != null ? user.getRole().name().toLowerCase() : "guest");
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            return dto;
        }
    }
}
