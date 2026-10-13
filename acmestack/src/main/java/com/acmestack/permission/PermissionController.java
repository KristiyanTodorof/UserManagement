package com.acmestack.permission;

import com.acmestack.audit.AuditService;
import com.acmestack.common.BusinessException;
import com.acmestack.config.AppUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/permissions")
public class PermissionController {

    private static final String EDIT = "permission_management.edit";

    private final PermissionService service;
    private final AuditService audit;

    public PermissionController(PermissionService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    // ---------- page ----------

    @GetMapping
    @PreAuthorize("hasAuthority('permission_management.view')")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(required = false) Long roleId,
                       @RequestParam(defaultValue = "") String review,
                       @AuthenticationPrincipal AppUserDetails me, Model model) {
        addMatrix(model, q, category, roleId, review, me);
        model.addAttribute("stats", service.stats());
        model.addAttribute("categories", service.categories());
        model.addAttribute("allRoles", service.allRoles());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("roleId", roleId);
        model.addAttribute("review", review);
        model.addAttribute("title", "Permissions");
        model.addAttribute("active", "permissions");
        return "permissions/list";
    }

    // ---------- HTMX fragments ----------

    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('permission_management.view')")
    public String matrix(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "") String category,
                         @RequestParam(required = false) Long roleId,
                         @RequestParam(defaultValue = "") String review,
                         @AuthenticationPrincipal AppUserDetails me, Model model) {
        addMatrix(model, q, category, roleId, review, me);
        return "permissions/matrix :: matrix";
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('permission_management.view')")
    public String stats(Model model) {
        model.addAttribute("stats", service.stats());
        return "permissions/stats :: stats";
    }

    @GetMapping("/{id}/panel")
    @PreAuthorize("hasAuthority('permission_management.view')")
    public String panel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, Model model) {
        Permission p = service.get(id);
        model.addAttribute("p", p);
        model.addAttribute("access", service.roleAccess(id));
        model.addAttribute("reviewDue", service.isReviewDue(p));
        model.addAttribute("events", audit.recentForTarget("Permission", id));
        model.addAttribute("canEdit", me.can(EDIT));
        return "permissions/panel :: panel";
    }

    // ---------- changes ----------

    /** Without an {@code effect} parameter the cell cycles to the next value. */
    @PostMapping("/{permissionId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('permission_management.edit')")
    public ResponseEntity<String> change(@PathVariable Long permissionId, @PathVariable Long roleId,
                                         @RequestParam(required = false) Effect effect,
                                         @AuthenticationPrincipal AppUserDetails me) {
        try {
            service.change(permissionId, roleId, effect, me.getName());
            return changed();
        } catch (BusinessException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('permission_management.edit')")
    public ResponseEntity<String> review(@PathVariable Long id) {
        service.markReviewed(id);
        return changed();
    }

    // ---------- helpers ----------

    /** Tells the stats, matrix and open panel to reload themselves. */
    private static ResponseEntity<String> changed() {
        return ResponseEntity.ok().header("HX-Trigger", "matrixChanged").build();
    }

    private void addMatrix(Model model, String q, String category, Long roleId, String review,
                           AppUserDetails me) {
        model.addAttribute("matrix", service.matrix(q, category, roleId, review));
        model.addAttribute("canEdit", me.can(EDIT));
    }
}