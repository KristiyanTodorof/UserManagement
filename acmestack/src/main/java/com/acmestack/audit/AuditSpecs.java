package com.acmestack.audit;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditSpecs {

    private AuditSpecs() {}

    public static Specification<AuditLog> filter(String q, EventType type, Long actorId,
                                                 Severity severity, LocalDateTime since) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (type != null) p.add(cb.equal(root.get("eventType"), type));
            if (actorId != null) p.add(cb.equal(root.get("actorId"), actorId));
            if (severity != null) p.add(cb.equal(root.get("severity"), severity));
            if (since != null) p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), since));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("actorName")), like),
                        cb.like(cb.lower(root.get("actorEmail")), like),
                        cb.like(cb.lower(root.get("targetLabel")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}