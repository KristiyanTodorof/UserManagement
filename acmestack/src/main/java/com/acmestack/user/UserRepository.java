package com.acmestack.user;

import com.acmestack.role.RoleCount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    long countByRoleId(Long roleId);
    long countByRoleIdAndDeletedAtIsNull(Long roleId);

    List<User> findTop3ByRoleIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long roleId);
    List<User> findTop20ByRoleIdAndDeletedAtIsNullOrderByNameAsc(Long roleId);

    @Query("""
    SELECT new com.acmestack.role.RoleCount(u.role.id, COUNT(u))
    FROM User u WHERE u.deletedAt IS NULL GROUP BY u.role.id
    """)
    List<RoleCount> countUsersPerRole();

    @Query("""
    SELECT u FROM User u JOIN FETCH u.role
    WHERE u.deletedAt IS NULL AND u.role.id <> :roleId ORDER BY u.name
    """)
    List<User> findAssignable(@Param("roleId") Long roleId);

    long countByStatusAndLastActiveAtGreaterThanEqualAndDeletedAtIsNull(UserStatus status, LocalDateTime from);
    long countByCreatedAtLessThanAndDeletedAtIsNull(LocalDateTime before);

    @Query("SELECT u.createdAt FROM User u WHERE u.deletedAt IS NULL AND u.createdAt >= :from")
    List<LocalDateTime> findCreatedAtSince(@Param("from") LocalDateTime from);

    @EntityGraph(attributePaths = "role")
    List<User> findTop5ByDeletedAtIsNullOrderByCreatedAtDescIdDesc();

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastActiveAt = :now WHERE u.id = :id")
    void touch(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("""
    SELECT u FROM User u JOIN FETCH u.role
    WHERE u.deletedAt IS NULL AND u.status <> com.acmestack.user.UserStatus.SUSPENDED AND u.id <> :uid
    ORDER BY u.name
    """)
    List<User> findChatCandidates(@Param("uid") Long uid);


    @Query("""
    SELECT u FROM User u JOIN FETCH u.role
    WHERE u.deletedAt IS NULL AND u.status <> com.acmestack.user.UserStatus.SUSPENDED
      AND u.id NOT IN (SELECT cm.user.id FROM ConversationMember cm WHERE cm.conversation.id = :cid)
    ORDER BY u.name
    """)
    List<User> findAddableToConversation(@Param("cid") Long cid);
}