package com.acmestack.dashboard;

import com.acmestack.audit.AuditLog;
import com.acmestack.audit.AuditLogRepository;
import com.acmestack.audit.EventType;
import com.acmestack.audit.Severity;
import com.acmestack.role.Role;
import com.acmestack.role.RoleCount;
import com.acmestack.role.RoleRepository;
import com.acmestack.user.User;
import com.acmestack.user.UserRepository;
import com.acmestack.user.UserStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int ONLINE_MINUTES = 15;
    private static final String[] COLORS =
            {"#2563eb", "#60a5fa", "#8b5cf6", "#f59e0b", "#10b981", "#ef4444", "#64748b"};

    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditLogRepository audit;

    public DashboardService(UserRepository users, RoleRepository roles, AuditLogRepository audit) {
        this.users = users;
        this.roles = roles;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        return new DashboardStats(
                users.countByDeletedAtIsNull(),
                users.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(weekAgo),
                users.countByStatusAndLastActiveAtGreaterThanEqualAndDeletedAtIsNull(
                        UserStatus.ACTIVE, now.minusMinutes(ONLINE_MINUTES)),
                users.countByStatusAndDeletedAtIsNull(UserStatus.PENDING),
                audit.countSeverityInRange(Severity.HIGH, weekAgo, now.plusMinutes(1)),
                audit.countSeverityInRange(Severity.HIGH, now.minusDays(14), weekAgo));
    }

    /** Cumulative number of users at the end of each day. */
    @Transactional(readOnly = true)
    public GrowthChart growth(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        LocalDateTime from = start.atStartOfDay();

        long running = users.countByCreatedAtLessThanAndDeletedAtIsNull(from);
        Map<LocalDate, Long> perDay = users.findCreatedAtSince(from).stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            running += perDay.getOrDefault(day, 0L);
            labels.add(day.format(fmt));
            values.add(running);
        }
        return new GrowthChart(labels, values);
    }

    @Transactional(readOnly = true)
    public List<RoleSlice> roleDistribution() {
        Map<Long, Long> counts = users.countUsersPerRole().stream()
                .collect(Collectors.toMap(RoleCount::roleId, RoleCount::count));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        List<RoleSlice> slices = new ArrayList<>();
        for (Role r : roles.findAll(Sort.by("id"))) {
            long n = counts.getOrDefault(r.getId(), 0L);
            if (n == 0) continue;
            int pct = total == 0 ? 0 : (int) Math.round(n * 100.0 / total);
            slices.add(new RoleSlice(r.getName(), n, pct, COLORS[slices.size() % COLORS.length]));
        }
        return slices;
    }

    /** Everything except plain sign-ins, which would drown the feed. */
    @Transactional(readOnly = true)
    public List<AuditLog> recentActivity() {
        return audit.findTop5ByEventTypeNotOrderByCreatedAtDescIdDesc(EventType.SIGN_IN);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recentSecurityEvents() {
        return audit.findTop3BySeverityOrderByCreatedAtDescIdDesc(Severity.HIGH);
    }

    @Transactional(readOnly = true)
    public List<User> recentUsers() {
        return users.findTop5ByDeletedAtIsNullOrderByCreatedAtDescIdDesc();
    }
}