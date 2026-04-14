package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ConversationDto;
import com.dxc.dxc_platform.dto.MessageDto;
import com.dxc.dxc_platform.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@PreAuthorize("isAuthenticated()")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations() {
        return ResponseEntity.ok(messageService.getUserConversations());
    }

    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<ConversationDto> getOrCreateConversation(@PathVariable Long otherUserId) {
        return ResponseEntity.ok(messageService.getOrCreateConversation(otherUserId));
    }

    @GetMapping("/conversation/{convId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long convId) {
        return ResponseEntity.ok(messageService.getConversationMessages(convId));
    }

    @PostMapping("/send/{receiverId}")
    public ResponseEntity<MessageDto> sendMessage(@PathVariable Long receiverId,
                                                  @RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        return ResponseEntity.ok(messageService.sendMessage(receiverId, content));
    }
}