package com.acmestack.role;

import com.acmestack.audit.AuditService;
import com.acmestack.common.BusinessException;
import com.acmestack.config.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/roles")
public class RoleController {

    private final RoleService service;
    private final AuditService audit;

    public RoleController(RoleService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    // ---------- list ----------

    @GetMapping
    @PreAuthorize("hasAuthority('role_management.view')")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String type,
                       @RequestParam(defaultValue = "name") String sort,
                       Model model) {
        model.addAttribute("rows", service.list(q, type, sort));
        model.addAttribute("stats", service.stats());
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("sort", sort);
        model.addAttribute("title", "Roles");
        model.addAttribute("active", "roles");
        return "roles/list";
    }

    // ---------- side panel (HTMX fragment) ----------

    @GetMapping("/{id}/panel")
    @PreAuthorize("hasAuthority('role_management.view')")
    public String panel(@PathVariable Long id,
                        @RequestParam(defaultValue = "overview") String tab,
                        Model model) {
        Role r = service.get(id);
        model.addAttribute("r", r);
        model.addAttribute("tab", tab);
        model.addAttribute("userCount", service.assignedCount(id));

        switch (tab) {
            case "overview", "permissions" -> {
                List<RolePermissionGroup> groups = service.permissionGroups(id);
                model.addAttribute("groups", groups);
                model.addAttribute("grantedTotal", groups.stream().mapToLong(RolePermissionGroup::granted).sum());
                model.addAttribute("permTotal", groups.stream().mapToLong(g -> g.items().size()).sum());
            }
            case "users" -> model.addAttribute("members", service.assignedUsers(id));
            case "activity" -> model.addAttribute("events", audit.recentForTarget("Role", id));
            default -> { }
        }
        return "roles/panel :: panel";
    }

    // ---------- create ----------

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('role_management.create')")
    public String newForm(Model model) {
        return formView(model, new RoleForm(), "Create role", false);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role_management.create')")
    public String create(@Valid @ModelAttribute("form") RoleForm form, BindingResult result,
                         Model model, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails me) {
        if (result.hasErrors()) return formView(model, form, "Create role", false);
        try {
            Role r = service.create(form, me.getName());
            ra.addFlashAttribute("message", "Role \"" + r.getName() + "\" created. It starts with no permissions.");
            return "redirect:/roles";
        } catch (BusinessException e) {
            result.rejectValue("name", "business", e.getMessage());
            return formView(model, form, "Create role", false);
        }
    }

    // ---------- edit ----------

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('role_management.edit')")
    public String editForm(@PathVariable Long id, Model model) {
        Role r = service.get(id);
        RoleForm f = new RoleForm();
        f.setId(r.getId());
        f.setName(r.getName());
        f.setDescription(r.getDescription());
        f.setIcon(r.getIcon());
        f.setColor(r.getColor());
        return formView(model, f, "Edit role", r.getType() == RoleType.SYSTEM);
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('role_management.edit')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RoleForm form,
                         BindingResult result, Model model, RedirectAttributes ra,
                         @AuthenticationPrincipal AppUserDetails me) {
        form.setId(id);
        boolean system = service.get(id).getType() == RoleType.SYSTEM;
        if (result.hasErrors()) return formView(model, form, "Edit role", system);
        try {
            service.update(id, form, me.getName());
            ra.addFlashAttribute("message", "Role updated.");
            return "redirect:/roles";
        } catch (BusinessException e) {
            result.rejectValue("name", "business", e.getMessage());
            return formView(model, form, "Edit role", system);
        }
    }

    // ---------- duplicate and delete ----------

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('role_management.create')")
    public String duplicate(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                            RedirectAttributes ra) {
        return run(ra, "Role duplicated.", () -> service.duplicate(id, me.getName()));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('role_management.delete')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        return run(ra, "Role deleted.", () -> service.delete(id));
    }

    // ---------- assign users ----------

    @GetMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('role_management.edit')")
    public String assignForm(@PathVariable Long id, Model model) {
        model.addAttribute("role", service.get(id));
        model.addAttribute("candidates", service.assignable(id));
        model.addAttribute("title", "Assign users");
        model.addAttribute("active", "roles");
        return "roles/assign";
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('role_management.edit')")
    public String assign(@PathVariable Long id, @RequestParam(required = false) List<Long> userIds,
                         @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        if (userIds == null || userIds.isEmpty()) {
            ra.addFlashAttribute("error", "Select at least one user.");
            return "redirect:/roles/" + id + "/assign";
        }
        AssignResult res = service.assign(id, userIds, me.getId());
        if (res.changed() > 0) {
            ra.addFlashAttribute("message", res.changed() + " user(s) assigned to " + service.get(id).getName() + ".");
        }
        if (!res.problems().isEmpty()) {
            ra.addFlashAttribute("error", String.join(" ", res.problems()));
        }
        return "redirect:/roles";
    }

    // ---------- helpers ----------

    private String run(RedirectAttributes ra, String success, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("message", success);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }

    private String formView(Model model, RoleForm form, String title, boolean systemRole) {
        model.addAttribute("form", form);
        model.addAttribute("icons", RoleService.ICONS);
        model.addAttribute("colors", RoleService.COLORS);
        model.addAttribute("systemRole", systemRole);
        model.addAttribute("title", title);
        model.addAttribute("active", "roles");
        return "roles/form";
    }
}