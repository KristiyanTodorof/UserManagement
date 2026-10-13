package com.acmestack.config;

import com.acmestack.settings.SettingsService;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

@Component
public class SessionTimeoutListener implements HttpSessionListener {

    private final SettingsService settings;

    public SessionTimeoutListener(SettingsService settings) {
        this.settings = settings;
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        event.getSession().setMaxInactiveInterval(settings.current().sessionTimeoutMinutes() * 60);
    }
}