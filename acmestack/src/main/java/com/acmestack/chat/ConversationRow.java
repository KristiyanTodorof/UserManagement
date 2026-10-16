package com.acmestack.chat;

import com.acmestack.user.User;

public record ConversationRow(Conversation conversation, String title, User other,
                              boolean muted, long unread, boolean online, String preview) {}