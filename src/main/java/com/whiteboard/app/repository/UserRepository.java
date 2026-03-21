package com.whiteboard.app.repository;

import com.whiteboard.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides standard CRUD operations plus custom queries for authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Search users whose username contains the given query (case-insensitive).
     * Used for the autocomplete/typeahead when adding team members.
     */
    List<User> findByUsernameContainingIgnoreCase(String query);

    /**
     * Find a user by their unique username.
     * Used during JWT authentication to load user details.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their unique email address.
     * Used during registration to check for duplicate emails.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a username is already taken.
     * Efficient existence check without loading the full entity.
     *
     * @param username the username to check
     * @return true if the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email is already registered.
     * Used during registration validation.
     *
     * @param email the email to check
     * @return true if the email is already in use
     */
    boolean existsByEmail(String email);
}
