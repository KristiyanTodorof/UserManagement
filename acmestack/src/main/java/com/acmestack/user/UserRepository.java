package com.acmestack.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    long countByDeletedAtIsNull();
    long countByStatusAndDeletedAtIsNull(UserStatus status);
    long countByRoleNameAndDeletedAtIsNull(String roleName);
    long countByRoleNameAndStatusAndDeletedAtIsNull(String roleName, UserStatus status);
    long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(LocalDateTime from);
    long countByCreatedAtBetweenAndDeletedAtIsNull(LocalDateTime from, LocalDateTime to);
}