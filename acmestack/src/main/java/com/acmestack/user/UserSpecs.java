package com.acmestack.user;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class UserSpecs {

    private UserSpecs() {}

    public static Specification<User> filter(String q, Long roleId, UserStatus status, LocalDateTime since) {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("role", JoinType.INNER);
            }

            List<Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (roleId != null) p.add(cb.equal(root.get("role").get("id"), roleId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (since != null) {
                p.add(cb.greaterThanOrEqualTo(
                        cb.coalesce(root.<LocalDateTime>get("lastActiveAt"), root.<LocalDateTime>get("createdAt")),
                        since));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}