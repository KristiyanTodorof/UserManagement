package com.acmestack.audit;

import com.acmestack.config.AppUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    public static final int PAGE_SIZE = 8;
    private static final int EXPORT_LIMIT = 10_000;
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    // ---------- writing ----------

    /** Logs a successful action by the currently signed-in user. */
    @Transactional
    public void log(EventType type, String description, String targetType, Long targetId,
                    String targetLabel, Map<String, Object> metadata) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails p) {
            save(p.getId(), p.getName(), p.getUsername(), type, AuditStatus.SUCCESS,
                    description, targetType, targetId, targetLabel, metadata);
        } else {
            save(null, "System", null, type, AuditStatus.SUCCESS,
                    description, targetType, targetId, targetLabel, metadata);
        }
    }

    /** Logs with an explicit actor, used for sign-ins (nobody is authenticated yet). */
    @Transactional
    public void logAs(Long actorId, String actorName, String actorEmail, EventType type,
                      AuditStatus status, String description, String targetType, Long targetId,
                      String targetLabel, Map<String, Object> metadata) {
        save(actorId, actorName, actorEmail, type, status, description, targetType, targetId,
                targetLabel, metadata);
    }

    @Transactional
    public void logExport(int rows) {
        log(EventType.REPORT_EXPORTED, "Exported user activity report", "Report", null,
                "Activity Logs", Map.of("format", "csv", "rows", rows));
    }

    private void save(Long actorId, String actorName, String actorEmail, EventType type,
                      AuditStatus status, String description, String targetType, Long targetId,
                      String targetLabel, Map<String, Object> extra) {
        RequestInfo req = RequestInfo.current();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("event_id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        if (extra != null) meta.putAll(extra);
        if (req.userAgent() != null) meta.put("user_agent", req.userAgent());

        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setActorName(cut(actorName == null ? "Unknown" : actorName, 100));
        a.setActorEmail(cut(actorEmail, 150));
        a.setEventType(type);
        a.setDescription(cut(description, 255));
        a.setTargetType(cut(targetType, 30));
        a.setTargetId(targetId);
        a.setTargetLabel(cut(targetLabel, 150));
        a.setSeverity(type.getSeverity());
        a.setStatus(status);
        a.setIpAddress(req.ip());
        a.setLocation(req.location());
        a.setDevice(req.device());
        a.setMetadata(toJson(meta));
        a.setCreatedAt(LocalDateTime.now());
        repo.save(a);
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String q, EventType type, Long actorId, Severity severity,
                                 String period, int page) {
        return repo.findAll(AuditSpecs.filter(q, type, actorId, severity, since(period)),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, NEWEST_FIRST));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> export(String q, EventType type, Long actorId, Severity severity, String period) {
        return repo.findAll(AuditSpecs.filter(q, type, actorId, severity, since(period)),
                PageRequest.of(0, EXPORT_LIMIT, NEWEST_FIRST)).getContent();
    }

    @Transactional(readOnly = true)
    public AuditLog get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recentForUser(Long userId) {
        return repo.findForUser(userId, PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public List<ActorOption> actors() {
        return repo.findActors();
    }

    @Transactional(readOnly = true)
    public ActivityStats stats() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime tomorrow = today.plusDays(1);
        return new ActivityStats(
                new DayStat(repo.countInRange(today, tomorrow), repo.countInRange(yesterday, today), false),
                new DayStat(count(EventType.FAILED_SIGN_IN, today, tomorrow), count(EventType.FAILED_SIGN_IN, yesterday, today), true),
                new DayStat(count(EventType.ROLE_CHANGED, today, tomorrow), count(EventType.ROLE_CHANGED, yesterday, today), false),
                new DayStat(count(EventType.REPORT_EXPORTED, today, tomorrow), count(EventType.REPORT_EXPORTED, yesterday, today), false));
    }

    // ---------- helpers ----------

    private long count(EventType type, LocalDateTime from, LocalDateTime to) {
        return repo.countTypesInRange(List.of(type), from, to);
    }

    private static LocalDateTime since(String period) {
        return switch (period) {
            case "1" -> LocalDateTime.now().minusDays(1);
            case "7" -> LocalDateTime.now().minusDays(7);
            case "30" -> LocalDateTime.now().minusDays(30);
            case "90" -> LocalDateTime.now().minusDays(90);
            default -> null;
        };
    }

    private String toJson(Map<String, Object> meta) {
        try {
            return cut(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta), 2000);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recentForTarget(String targetType, Long targetId) {
        return repo.findForTarget(targetType, targetId, PageRequest.of(0, 10));
    }
}