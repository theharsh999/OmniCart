package com.omnicart.repository;

import com.omnicart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
    java.util.List<User> findByRoleOrderByNameAsc(com.omnicart.entity.Role role);
    java.util.List<User> findByNameContainingIgnoreCaseAndRole(String name, com.omnicart.entity.Role role);
    java.util.Optional<User> findByNameIgnoreCaseAndRole(String name, com.omnicart.entity.Role role);
}
