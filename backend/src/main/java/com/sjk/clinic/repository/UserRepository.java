package com.sjk.clinic.repository;

import com.sjk.clinic.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR :username = '' OR u.username LIKE CONCAT('%', :username, '%')) AND " +
           "(:realName IS NULL OR :realName = '' OR u.realName LIKE CONCAT('%', :realName, '%')) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findBySearchCriteria(
            @Param("username") String username,
            @Param("realName") String realName,
            @Param("role") User.UserRole role,
            @Param("status") User.UserStatus status,
            Pageable pageable);
}
