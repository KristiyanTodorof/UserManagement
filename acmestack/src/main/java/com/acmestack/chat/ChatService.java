package com.acmestack.chat;

import com.acmestack.audit.AuditService;
import com.acmestack.audit.EventType;
import com.acmestack.common.BusinessException;
import com.acmestack.role.RoleService;
import com.acmestack.user.User;
import com.acmestack.user.UserRepository;
import com.acmestack.user.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    public static final int PAGE = 50;
    public static final int MAX_TEXT = 2000;
    public static final long MAX_FILE = 5L * 1024 * 1024;
    public static final List<String> GROUP_ICONS = List.of("bi-people", "bi-palette", "bi-megaphone",
            "bi-headset", "bi-layers", "bi-rocket-takeoff", "bi-chat-dots", "bi-briefcase");

    public record Shared(List<Message> files, List<Message> media) {}
    public record Attachment(Path path, String name, String contentType, boolean inline, long size) {}

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final UserRepository users;
    private final AuditService audit;
    private final Path root;

    public ChatService(ConversationRepository conversations, ConversationMemberRepository members,
                       MessageRepository messages, UserRepository users, AuditService audit,
                       @Value("${app.chat.upload-dir:}") String uploadDir) {
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.users = users;
        this.audit = audit;
        Path dir = uploadDir == null || uploadDir.isBlank()
                ? Path.of(System.getProperty("user.home"), "acmestack-uploads")
                : Path.of(uploadDir);
        this.root = dir.toAbsolutePath().normalize();
    }

    // ---------- the conversation list ----------

    @Transactional(readOnly = true)
    public List<ConversationRow> conversations(Long uid) {
        List<ConversationMember> mine = members.findForUser(uid);
        if (mine.isEmpty()) return List.of();

        Map<Long, Long> unread = messages.countUnread(uid).stream()
                .collect(Collectors.toMap(ConvCount::conversationId, ConvCount::count));

        List<Long> directIds = mine.stream().map(ConversationMember::getConversation)
                .filter(c -> c.getType() == ConversationType.DIRECT).map(Conversation::getId).toList();
        Map<Long, User> others = directIds.isEmpty() ? Map.of()
                : members.findOthersInDirects(directIds, uid).stream()
                .collect(Collectors.toMap(cm -> cm.getConversation().getId(),
                        ConversationMember::getUser, (a, b) -> a));

        LocalDateTime recent = LocalDateTime.now().minusMinutes(5);
        return mine.stream().map(cm -> {
            Conversation c = cm.getConversation();
            User other = others.get(c.getId());
            String title = c.getType() == ConversationType.GROUP ? c.getName()
                    : (other != null ? other.getName() : "Unknown");
            boolean online = other != null && other.getStatus() == UserStatus.ACTIVE
                    && other.getLastActiveAt() != null && other.getLastActiveAt().isAfter(recent);
            return new ConversationRow(c, title, other, cm.isMuted(),
                    unread.getOrDefault(c.getId(), 0L), online, preview(c, uid));
        }).toList();
    }

    public static List<ConversationRow> filter(List<ConversationRow> rows, String filter, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return rows.stream()
                .filter(r -> switch (filter) {
                    case "direct" -> r.conversation().getType() == ConversationType.DIRECT;
                    case "groups" -> r.conversation().getType() == ConversationType.GROUP;
                    case "unread" -> r.unread() > 0;
                    default -> true;
                })
                .filter(r -> needle.isEmpty() || r.title().toLowerCase().contains(needle))
                .toList();
    }

    private static String preview(Conversation c, Long uid) {
        if (c.getLastMessagePreview() == null) return "No messages yet";
        boolean mine = uid.equals(c.getLastMessageSenderId());
        String who = mine ? "You" : first(c.getLastMessageSenderName());
        return (c.getType() == ConversationType.GROUP || mine)
                ? who + ": " + c.getLastMessagePreview() : c.getLastMessagePreview();
    }

    private static String first(String name) {
        if (name == null || name.isBlank()) return "Someone";
        return name.trim().split("\\s+")[0];
    }

    // ---------- reading a conversation ----------

    @Transactional
    public List<Message> thread(Long cid, Long uid) {
        membership(cid, uid);
        List<Message> latest = new ArrayList<>(messages.findLatest(cid, PageRequest.of(0, PAGE)));
        Collections.reverse(latest);
        if (!latest.isEmpty()) members.markRead(cid, uid, latest.get(latest.size() - 1).getId());
        return latest;
    }

    @Transactional
    public List<Message> after(Long cid, Long uid, long after) {
        membership(cid, uid);
        List<Message> list = messages.findAfter(cid, after, PageRequest.of(0, 200));
        if (!list.isEmpty()) members.markRead(cid, uid, list.get(list.size() - 1).getId());
        return list;
    }

    @Transactional(readOnly = true)
    public List<User> members(Long cid, Long uid) {
        membership(cid, uid);
        return members.findMembers(cid).stream().map(ConversationMember::getUser).toList();
    }

    @Transactional(readOnly = true)
    public Shared shared(Long cid, Long uid) {
        membership(cid, uid);
        List<Message> all = messages.findAttachments(cid, PageRequest.of(0, 40));
        return new Shared(
                all.stream().filter(m -> !FileTypes.isImage(m.getAttachmentName())).limit(3).toList(),
                all.stream().filter(m -> FileTypes.isImage(m.getAttachmentName())).limit(4).toList());
    }

    @Transactional(readOnly = true)
    public List<User> candidates(Long uid) {
        return users.findChatCandidates(uid);
    }

    // ---------- writing ----------

    @Transactional
    public void send(Long cid, Long uid, String body, MultipartFile file) {
        ConversationMember cm = membership(cid, uid);
        String text = body == null ? "" : body.strip();
        if (text.length() > MAX_TEXT) throw new BusinessException("The message is too long (max 2000 characters).");
        boolean hasFile = file != null && !file.isEmpty();
        if (text.isEmpty() && !hasFile) throw new BusinessException("Type a message or attach a file.");

        User sender = users.findById(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Message m = new Message();
        m.setConversation(cm.getConversation());
        m.setSender(sender);
        m.setBody(text);
        m.setCreatedAt(LocalDateTime.now());

        String stored = null;
        try {
            if (hasFile) {
                String name = FileTypes.cleanName(file.getOriginalFilename());
                if (!FileTypes.allowed(name)) {
                    throw new BusinessException("This file type isn't allowed. Allowed: PDF, images, TXT, CSV, Word, Excel, PowerPoint, ZIP.");
                }
                if (file.getSize() > MAX_FILE) throw new BusinessException("The file is too large. The limit is 5 MB.");
                stored = store(file);
                m.setAttachmentName(name);
                m.setAttachmentPath(stored);
                m.setAttachmentSize(file.getSize());
            }
            messages.save(m);
        } catch (RuntimeException e) {
            deleteQuietly(stored);
            throw e;
        }

        Conversation c = cm.getConversation();     // managed, затова промените се записват при commit
        String shown = text.isEmpty() ? "Attachment: " + m.getAttachmentName() : text.replaceAll("\\s+", " ");
        c.setLastMessageAt(m.getCreatedAt());
        c.setLastMessagePreview(shown.length() > 120 ? shown.substring(0, 120) : shown);
        c.setLastMessageSenderId(sender.getId());
        c.setLastMessageSenderName(sender.getName());
    }

    @Transactional
    public Long openDirect(Long uid, Long otherId) {
        if (uid.equals(otherId)) throw new BusinessException("You can't chat with yourself.");
        User other = users.findActiveById(otherId)
                .orElseThrow(() -> new BusinessException("User not found."));
        if (other.getStatus() == UserStatus.SUSPENDED) throw new BusinessException("This user is suspended.");

        String key = Math.min(uid, otherId) + ":" + Math.max(uid, otherId);
        Optional<Conversation> existing = conversations.findByDirectKey(key);
        if (existing.isPresent()) return existing.get().getId();

        LocalDateTime now = LocalDateTime.now();
        Conversation c = new Conversation();
        c.setType(ConversationType.DIRECT);
        c.setDirectKey(key);
        c.setIcon("bi-person");
        c.setColor("blue");
        c.setCreatedBy(uid);
        c.setCreatedAt(now);
        conversations.save(c);
        addMember(c, users.getReferenceById(uid), now);
        addMember(c, other, now);
        return c.getId();
    }

    @Transactional
    public Long createGroup(Long uid, String name, String icon, String color, List<Long> memberIds) {
        String n = name == null ? "" : name.strip();
        if (n.isEmpty() || n.length() > 60) throw new BusinessException("Group name is required (max 60 characters).");

        Set<Long> ids = memberIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(memberIds);
        ids.remove(uid);
        if (ids.isEmpty()) throw new BusinessException("Pick at least one other member.");
        if (ids.size() > 49) throw new BusinessException("A group can have at most 50 members.");

        List<User> found = users.findAllById(ids).stream()
                .filter(u -> u.getDeletedAt() == null && u.getStatus() != UserStatus.SUSPENDED).toList();
        if (found.size() != ids.size()) throw new BusinessException("One or more selected users are not available.");

        LocalDateTime now = LocalDateTime.now();
        Conversation c = new Conversation();
        c.setType(ConversationType.GROUP);
        c.setName(n);
        c.setIcon(GROUP_ICONS.contains(icon) ? icon : GROUP_ICONS.get(0));
        c.setColor(RoleService.COLORS.contains(color) ? color : "blue");   // само познати стойности: стават CSS класове
        c.setCreatedBy(uid);
        c.setCreatedAt(now);
        conversations.save(c);

        addMember(c, users.getReferenceById(uid), now);
        for (User u : found) addMember(c, u, now);

        audit.log(EventType.CHAT_GROUP_CREATED, "Created a group chat", "Conversation", c.getId(), n,
                Map.of("members", found.size() + 1));
        return c.getId();
    }

    @Transactional
    public void setMuted(Long cid, Long uid, boolean muted) {
        membership(cid, uid).setMuted(muted);
    }

    // ---------- files ----------

    @Transactional(readOnly = true)
    public Attachment attachment(Long messageId, Long uid) {
        Message m = messages.findWithConversation(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        membership(m.getConversation().getId(), uid);
        if (m.getAttachmentPath() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        Path p = root.resolve(m.getAttachmentPath()).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        String name = m.getAttachmentName();
        return new Attachment(p, name, FileTypes.contentType(name), FileTypes.isImage(name),
                m.getAttachmentSize() == null ? 0 : m.getAttachmentSize());
    }

    // ---------- helpers ----------

    private ConversationMember membership(Long cid, Long uid) {
        return members.findMembership(cid, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void addMember(Conversation c, User u, LocalDateTime now) {
        ConversationMember cm = new ConversationMember();
        cm.setConversation(c);
        cm.setUser(u);
        cm.setLastReadMessageId(0);
        cm.setMuted(false);
        cm.setJoinedAt(now);
        members.save(cm);
    }

    private String store(MultipartFile file) {
        try {
            Files.createDirectories(root);
            String id = UUID.randomUUID().toString().replace("-", "");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, root.resolve(id));
            }
            return id;
        } catch (IOException e) {
            throw new BusinessException("The file could not be saved.");
        }
    }

    private void deleteQuietly(String stored) {
        if (stored == null) return;
        try {
            Files.deleteIfExists(root.resolve(stored));
        } catch (IOException ignored) {
            // нищо повече за правене
        }
    }
}