package com.acmestack.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :from AND a.createdAt < :to")
    long countInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.eventType IN :types AND a.createdAt >= :from AND a.createdAt < :to
        """)
    long countTypesInRange(@Param("types") Collection<EventType> types,
                           @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.actorId = :id OR (a.targetType = 'User' AND a.targetId = :id)
        ORDER BY a.createdAt DESC, a.id DESC
        """)
    List<AuditLog> findForUser(@Param("id") Long id, Pageable pageable);

    @Query("""
        SELECT new com.acmestack.audit.ActorOption(a.actorId, a.actorName)
        FROM AuditLog a WHERE a.actorId IS NOT NULL
        GROUP BY a.actorId, a.actorName ORDER BY a.actorName
        """)
    List<ActorOption> findActors();
}