package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for users.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used for login and for loading the authenticated user. */
    Optional<User> findByEmail(String email);

    /** Used during registration to reject duplicate emails. */
    boolean existsByEmail(String email);
}
