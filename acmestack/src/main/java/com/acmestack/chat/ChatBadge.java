package com.acmestack.chat;

import com.acmestack.config.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("chatBadge")
public class ChatBadge {

    private final MessageRepository messages;

    public ChatBadge(MessageRepository messages) {
        this.messages = messages;
    }

    public long unread() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails me) || !me.can("chat.use")) {
            return 0;
        }
        return messages.countUnreadTotal(me.getId());
    }
}