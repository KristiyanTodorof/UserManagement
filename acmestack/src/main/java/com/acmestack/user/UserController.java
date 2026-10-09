package com.acmestack.user;

import com.acmestack.common.BusinessException;
import com.acmestack.config.AppUserDetails;
import com.acmestack.role.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    private final RoleRepository roles;

    public UserController(UserService service, RoleRepository roles) {
        this.service = service;
        this.roles = roles;
    }

    // ---------- list ----------

    @GetMapping
    @PreAuthorize("hasAuthority('user_management.view')")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) Long roleId,
                       @RequestParam(required = false) UserStatus status,
                       @RequestParam(defaultValue = "all") String period,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<User> result = service.search(q, roleId, status, period, page);
        model.addAttribute("page", result);
        model.addAttribute("stats", service.stats());
        model.addAttribute("roles", roles.findAll());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("period", period);
        model.addAttribute("title", "Users");
        model.addAttribute("active", "users");
        return "users/list";
    }

    // ---------- side panel (HTMX fragment) ----------

    @GetMapping("/{id}/panel")
    @PreAuthorize("hasAuthority('user_management.view')")
    public String panel(@PathVariable Long id,
                        @RequestParam(defaultValue = "overview") String tab,
                        @AuthenticationPrincipal AppUserDetails me,
                        Model model) {
        User u = service.get(id);
        model.addAttribute("u", u);
        model.addAttribute("tab", tab);
        model.addAttribute("self", u.getId().equals(me.getId()));
        model.addAttribute("groups", service.permissionGroups(u.getRole().getId()));
        return "users/panel :: panel";
    }

    // ---------- add ----------

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('user_management.create')")
    public String newForm(Model model) {
        return formView(model, new UserForm(), "Add user");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user_management.create')")
    public String create(@Valid @ModelAttribute("form") UserForm form, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, form, "Add user");
        try {
            String temp = service.invite(form.getName(), form.getEmail(), form.getRoleId());
            ra.addFlashAttribute("message",
                    "Invited " + form.getEmail() + ". Temporary password (shown once): " + temp);
            return "redirect:/users";
        } catch (BusinessException e) {
            result.rejectValue("email", "business", e.getMessage());
            return formView(model, form, "Add user");
        }
    }

    // ---------- edit ----------

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('user_management.edit')")
    public String editForm(@PathVariable Long id, Model model) {
        User u = service.get(id);
        UserForm form = new UserForm();
        form.setId(u.getId());
        form.setName(u.getName());
        form.setEmail(u.getEmail());
        form.setRoleId(u.getRole().getId());
        return formView(model, form, "Edit user");
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('user_management.edit')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") UserForm form,
                         BindingResult result, Model model, RedirectAttributes ra,
                         @AuthenticationPrincipal AppUserDetails me) {
        form.setId(id);
        if (result.hasErrors()) return formView(model, form, "Edit user");
        try {
            service.update(id, form.getName(), form.getEmail(), form.getRoleId(), me.getId());
            ra.addFlashAttribute("message", "User updated.");
            return "redirect:/users";
        } catch (BusinessException e) {
            result.rejectValue("email", "business", e.getMessage());
            return formView(model, form, "Edit user");
        }
    }

    // ---------- actions ----------

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('user_management.suspend')")
    public String suspend(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                          RedirectAttributes ra) {
        return run(ra, "User suspended.", () -> service.suspend(id, me.getId()));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('user_management.suspend')")
    public String reactivate(@PathVariable Long id, RedirectAttributes ra) {
        return run(ra, "User reactivated.", () -> service.reactivate(id));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('user_management.delete')")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                         RedirectAttributes ra) {
        return run(ra, "User deleted.", () -> service.delete(id, me.getId()));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user_management.edit')")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        try {
            String temp = service.resetPassword(id);
            ra.addFlashAttribute("message", "Password reset. New temporary password (shown once): " + temp);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    // ---------- helpers ----------

    private String run(RedirectAttributes ra, String success, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("message", success);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    private String formView(Model model, UserForm form, String title) {
        model.addAttribute("form", form);
        model.addAttribute("roles", roles.findAll());
        model.addAttribute("title", title);
        model.addAttribute("active", "users");
        return "users/form";
    }
}