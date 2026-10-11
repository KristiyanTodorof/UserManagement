package com.acmestack.audit;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/activity")
public class ActivityController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditService service;

    public ActivityController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit.view')")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) EventType type,
                       @RequestParam(required = false) Long actorId,
                       @RequestParam(required = false) Severity severity,
                       @RequestParam(defaultValue = "30") String period,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<AuditLog> result = service.search(q, type, actorId, severity, period, page);
        model.addAttribute("page", result);
        model.addAttribute("stats", service.stats());
        model.addAttribute("actors", service.actors());
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("actorId", actorId);
        model.addAttribute("severity", severity);
        model.addAttribute("period", period);
        model.addAttribute("title", "Activity");
        model.addAttribute("active", "activity");
        return "activity/list";
    }

    @GetMapping("/{id}/panel")
    @PreAuthorize("hasAuthority('audit.view')")
    public String panel(@PathVariable Long id, Model model) {
        model.addAttribute("e", service.get(id));
        return "activity/panel :: panel";
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('audit.export')")
    public void export(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) EventType type,
                       @RequestParam(required = false) Long actorId,
                       @RequestParam(required = false) Severity severity,
                       @RequestParam(defaultValue = "30") String period,
                       HttpServletResponse response) throws IOException {
        List<AuditLog> rows = service.export(q, type, actorId, severity, period);
        service.logExport(rows.size());

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"activity-log-" + LocalDate.now() + ".csv\"");

        PrintWriter w = response.getWriter();
        w.write('\uFEFF');   // BOM so Excel reads the file as UTF-8
        w.println("Timestamp,Actor,Actor email,Event,Description,Target,Severity,Status,IP address,Device");
        for (AuditLog a : rows) {
            w.println(String.join(",",
                    csv(a.getCreatedAt().format(TS)), csv(a.getActorName()), csv(a.getActorEmail()),
                    csv(a.getEventType().getLabel()), csv(a.getDescription()), csv(a.getTargetLabel()),
                    csv(a.getSeverity().name()), csv(a.getStatus().name()),
                    csv(a.getIpAddress()), csv(a.getDevice())));
        }
        w.flush();
    }

    /** Quotes the value, and defuses spreadsheet formulas (a typed email like "=cmd|..." at login). */
    private static String csv(String v) {
        if (v == null) return "\"\"";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}