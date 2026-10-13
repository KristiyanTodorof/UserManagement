package com.acmestack.common;

import com.acmestack.settings.SettingsService;
import com.acmestack.settings.WorkspaceInfo;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModel {

    private final SettingsService settings;

    public GlobalModel(SettingsService settings) {
        this.settings = settings;
    }

    @ModelAttribute("ws")
    public WorkspaceInfo workspace() {
        return settings.current();
    }
}