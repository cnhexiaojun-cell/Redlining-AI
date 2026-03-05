package com.redlining.repository;

import com.redlining.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    default Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return Optional.empty();
        }
        String s = usernameOrEmail.trim();
        return findByUsername(s).or(() -> findByEmailIgnoreCase(s));
    }
}
