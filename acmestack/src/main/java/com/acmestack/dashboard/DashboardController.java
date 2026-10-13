package com.acmestack.dashboard;

import com.acmestack.config.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class DashboardController {

    private final DashboardService service;
    private final HealthService healthService;

    public DashboardController(DashboardService service, HealthService healthService) {
        this.service = service;
        this.healthService = healthService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('reports.view')")
    public String dashboard(@RequestParam(defaultValue = "30") int days,
                            @AuthenticationPrincipal AppUserDetails me, Model model) {
        int range = (days == 7 || days == 30 || days == 90) ? days : 30;

        GrowthChart growth = service.growth(range);
        List<RoleSlice> slices = service.roleDistribution();

        model.addAttribute("stats", service.stats());
        model.addAttribute("days", range);
        model.addAttribute("growth", growth);
        model.addAttribute("slices", slices);
        model.addAttribute("sliceNames", slices.stream().map(RoleSlice::name).toList());
        model.addAttribute("sliceCounts", slices.stream().map(RoleSlice::count).toList());
        model.addAttribute("sliceColors", slices.stream().map(RoleSlice::color).toList());
        model.addAttribute("roleTotal", slices.stream().mapToLong(RoleSlice::count).sum());
        model.addAttribute("health", healthService.check());

        // only load what this user is allowed to see
        if (me.can("audit.view")) {
            model.addAttribute("events", service.recentActivity());
            model.addAttribute("alerts", service.recentSecurityEvents());
        }
        if (me.can("user_management.view")) {
            model.addAttribute("recentUsers", service.recentUsers());
        }

        model.addAttribute("title", "Dashboard");
        model.addAttribute("active", "dashboard");
        return "dashboard";
    }
}