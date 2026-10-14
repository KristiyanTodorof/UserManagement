package com.acmestack.common;

import com.acmestack.config.AppUserDetails;
import com.acmestack.config.LandingPages;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AppUserDetails me) {
        return "redirect:" + LandingPages.forUser(me);
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/no-access")
    public String noAccess(@AuthenticationPrincipal AppUserDetails me) {
        String landing = LandingPages.forUser(me);
        return "/no-access".equals(landing) ? "no-access" : "redirect:" + landing;
    }
}