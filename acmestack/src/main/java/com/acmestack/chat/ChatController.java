package com.acmestack.chat;

import com.acmestack.common.BusinessException;
import com.acmestack.config.AppUserDetails;
import com.acmestack.role.RoleService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/chat")
@PreAuthorize("hasAuthority('chat.use')")
public class ChatController {

    private static final Map<String, String> FILTERS = new LinkedHashMap<>();
    static {
        FILTERS.put("all", "All");
        FILTERS.put("direct", "Direct");
        FILTERS.put("groups", "Groups");
        FILTERS.put("unread", "Unread");
    }

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    // ---------- the page ----------

    @GetMapping
    public String page(@RequestParam(required = false) Long c,
                       @RequestParam(defaultValue = "all") String filter,
                       @RequestParam(defaultValue = "") String q,
                       @AuthenticationPrincipal AppUserDetails me, Model model) {
        List<ConversationRow> all = service.conversations(me.getId());

        ConversationRow current = null;
        if (c != null) {
            current = all.stream().filter(r -> r.conversation().getId().equals(c)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } else if (!all.isEmpty()) {
            current = all.get(0);
        }

        if (current != null) {
            Long id = current.conversation().getId();
            List<Message> messages = service.thread(id, me.getId());
            model.addAttribute("current", current);
            model.addAttribute("activeId", id);
            model.addAttribute("messages", messages);
            model.addAttribute("after", messages.isEmpty() ? 0L : messages.get(messages.size() - 1).getId());
            model.addAttribute("shared", service.shared(id, me.getId()));
            if (current.conversation().getType() == ConversationType.GROUP) {
                model.addAttribute("members", service.members(id, me.getId()));
            }
        }

        model.addAttribute("rows", ChatService.filter(all, filter, q));
        model.addAttribute("filters", FILTERS);
        model.addAttribute("filter", filter);
        model.addAttribute("q", q);
        model.addAttribute("meId", me.getId());
        model.addAttribute("canGroups", me.can("chat.groups"));
        model.addAttribute("accept", FileTypes.ACCEPT);
        model.addAttribute("title", "Chat");
        model.addAttribute("active", "chat");
        return "chat/index";
    }

    // ---------- HTMX fragments ----------

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Long c,
                       @RequestParam(defaultValue = "all") String filter,
                       @RequestParam(defaultValue = "") String q,
                       @AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("rows", ChatService.filter(service.conversations(me.getId()), filter, q));
        model.addAttribute("activeId", c);
        model.addAttribute("filter", filter);
        model.addAttribute("q", q);
        return "chat/list :: list";
    }

    @GetMapping("/{id}/messages")
    public String messages(@PathVariable Long id, @RequestParam(defaultValue = "0") long after,
                           @AuthenticationPrincipal AppUserDetails me, Model model) {
        List<Message> list = service.after(id, me.getId(), after);
        model.addAttribute("messages", list);
        model.addAttribute("meId", me.getId());
        model.addAttribute("activeId", id);
        model.addAttribute("after", list.isEmpty() ? after : list.get(list.size() - 1).getId());
        return "chat/messages :: batch";
    }

    // ---------- sending ----------

    @PostMapping("/{id}/send")
    public ResponseEntity<String> send(@PathVariable Long id,
                                       @RequestParam(defaultValue = "") String body,
                                       @RequestParam(required = false) MultipartFile file,
                                       @AuthenticationPrincipal AppUserDetails me) {
        try {
            service.send(id, me.getId(), body, file);
            return ResponseEntity.ok().header("HX-Trigger", "messageSent").build();
        } catch (BusinessException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/mute")
    public String mute(@PathVariable Long id, @RequestParam boolean muted,
                       @AuthenticationPrincipal AppUserDetails me) {
        service.setMuted(id, me.getId(), muted);
        return "redirect:/chat?c=" + id;
    }

    // ---------- starting conversations ----------

    @GetMapping("/new")
    public String newChat(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("candidates", service.candidates(me.getId()));
        model.addAttribute("title", "New chat");
        model.addAttribute("active", "chat");
        return "chat/new";
    }

    @PostMapping("/direct/{userId}")
    public String direct(@PathVariable Long userId, @AuthenticationPrincipal AppUserDetails me,
                         RedirectAttributes ra) {
        try {
            Long id;
            try {
                id = service.openDirect(me.getId(), userId);
            } catch (DataIntegrityViolationException e) {   // двама души са отворили един и същи чат едновременно
                id = service.openDirect(me.getId(), userId);
            }
            return "redirect:/chat?c=" + id;
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/chat/new";
        }
    }

    @GetMapping("/groups/new")
    @PreAuthorize("hasAuthority('chat.use') and hasAuthority('chat.groups')")
    public String groupForm(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("candidates", service.candidates(me.getId()));
        model.addAttribute("icons", ChatService.GROUP_ICONS);
        model.addAttribute("colors", RoleService.COLORS);
        model.addAttribute("title", "New group");
        model.addAttribute("active", "chat");
        return "chat/group-form";
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('chat.use') and hasAuthority('chat.groups')")
    public String createGroup(@RequestParam String name,
                              @RequestParam(defaultValue = "bi-people") String icon,
                              @RequestParam(defaultValue = "blue") String color,
                              @RequestParam(required = false) List<Long> memberIds,
                              @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        try {
            return "redirect:/chat?c=" + service.createGroup(me.getId(), name, icon, color, memberIds);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/chat/groups/new";
        }
    }

    // ---------- files ----------

    @GetMapping("/files/{messageId}")
    public ResponseEntity<Resource> file(@PathVariable Long messageId, @AuthenticationPrincipal AppUserDetails me) {
        ChatService.Attachment a = service.attachment(messageId, me.getId());
        ContentDisposition disposition = ContentDisposition.builder(a.inline() ? "inline" : "attachment")
                .filename(a.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(a.contentType()))
                .contentLength(a.size())
                .body(new FileSystemResource(a.path()));
    }
}