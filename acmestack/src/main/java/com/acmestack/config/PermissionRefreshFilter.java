package com.acmestack.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Re-reads the signed-in user on every request so role and permission changes apply immediately. */
public class PermissionRefreshFilter extends OncePerRequestFilter {

    private final AppUserDetailsService users;
    private final ActivityTracker tracker;

    public PermissionRefreshFilter(AppUserDetailsService users, ActivityTracker tracker) {
        this.users = users;
        this.tracker = tracker;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserDetails current) {
            Optional<AppUserDetails> fresh = users.loadById(current.getId());

            // deleted or suspended since the last request: sign them out
            if (fresh.isEmpty() || !fresh.get().isAccountNonLocked()) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();

                String login = request.getContextPath() + "/login";
                if ("true".equals(request.getHeader("HX-Request"))) {
                    response.setHeader("HX-Redirect", login);
                } else {
                    response.sendRedirect(login);
                }
                return;
            }

            tracker.touch(fresh.get().getId());

            UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken
                    .authenticated(fresh.get(), null, fresh.get().getAuthorities());
            token.setDetails(auth.getDetails());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(token);
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}