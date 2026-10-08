package com.acmestack.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    @Query("""
    SELECT rp.permission.key FROM RolePermission rp
    WHERE rp.role.id = :roleId AND rp.effect = com.acmestack.permission.Effect.GRANTED
    """)
    List<String> findGrantedKeys(@Param("roleId") Long roleId);
}