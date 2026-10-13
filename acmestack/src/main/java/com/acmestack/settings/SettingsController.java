package com.acmestack.settings;

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
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings.view')")
    public String view(@AuthenticationPrincipal AppUserDetails me, Model model) {
        return page(model, service.form(), me);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings.edit')")
    public String save(@Valid @ModelAttribute("form") SettingsForm form, BindingResult result,
                       @AuthenticationPrincipal AppUserDetails me, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return page(model, form, me);
        try {
            boolean changed = service.save(form, me.getName());
            ra.addFlashAttribute("message", changed ? "Settings saved." : "Nothing to save, no changes were made.");
            return "redirect:/settings";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return page(model, form, me);
        }
    }

    @PostMapping("/integrations/{id}/toggle")
    @PreAuthorize("hasAuthority('settings.edit')")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        service.toggleIntegration(id);
        ra.addFlashAttribute("message", "Integration updated (demo only, nothing is really connected).");
        return "redirect:/settings#integrations";
    }

    private String page(Model model, SettingsForm form, AppUserDetails me) {
        List<SecurityCheck> checks = service.securityChecks();
        model.addAttribute("form", form);
        model.addAttribute("zones", SettingsService.ZONES);
        model.addAttribute("timeouts", SettingsService.TIMEOUTS);
        model.addAttribute("emailLevels", SettingsService.EMAIL_LEVELS);
        model.addAttribute("productLevels", SettingsService.PRODUCT_LEVELS);
        model.addAttribute("themes", SettingsService.THEMES);
        model.addAttribute("checks", checks);
        model.addAttribute("secure", checks.stream().allMatch(SecurityCheck::ok));
        model.addAttribute("integrations", service.integrations());
        model.addAttribute("canEdit", me.can("settings.edit"));
        model.addAttribute("title", "Settings");
        model.addAttribute("active", "settings");
        return "settings/index";
    }
}