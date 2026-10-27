package com.luxora.commerce.user.repository;

import com.luxora.commerce.user.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    java.util.List<User> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(Instant since);
}
