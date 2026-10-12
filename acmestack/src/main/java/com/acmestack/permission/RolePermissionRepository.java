package com.acmestack.permission;

import com.acmestack.role.RoleCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    @Query("""
    SELECT rp.permission.key FROM RolePermission rp
    WHERE rp.role.id = :roleId AND rp.effect = com.acmestack.permission.Effect.GRANTED
    """)
    List<String> findGrantedKeys(@Param("roleId") Long roleId);

    @Query("""
    SELECT rp FROM RolePermission rp JOIN FETCH rp.permission
    WHERE rp.role.id = :roleId ORDER BY rp.permission.id
    """)
    List<RolePermission> findWithPermissionByRoleId(@Param("roleId") Long roleId);

    @Query("""
    SELECT new com.acmestack.role.RoleCount(rp.role.id, COUNT(rp))
    FROM RolePermission rp
    WHERE rp.effect = com.acmestack.permission.Effect.GRANTED
    GROUP BY rp.role.id
    """)
    List<RoleCount> countGrantedByRole();

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.role JOIN FETCH rp.permission")
    List<RolePermission> findAllWithRefs();

    @Query("""
    SELECT rp FROM RolePermission rp JOIN FETCH rp.role
    WHERE rp.permission.id = :permissionId ORDER BY rp.role.id
    """)
    List<RolePermission> findByPermissionIdWithRole(@Param("permissionId") Long permissionId);

    long countByEffect(Effect effect);
}