package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ConversationDto;
import com.dxc.dxc_platform.dto.MessageDto;
import com.dxc.dxc_platform.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                                                  @RequestBody Map<String, Object> payload) {
        String content = payload.get("content") != null ? String.valueOf(payload.get("content")) : "";
        String clientTempId = payload.get("clientTempId") != null ? String.valueOf(payload.get("clientTempId")) : null;

        Long replyToMessageId = null;
        if (payload.get("replyToMessageId") != null) {
            Object raw = payload.get("replyToMessageId");
            if (raw instanceof Number) {
                replyToMessageId = ((Number) raw).longValue();
            } else {
                replyToMessageId = Long.parseLong(String.valueOf(raw));
            }
        }

        return ResponseEntity.ok(
                messageService.sendMessage(receiverId, content, clientTempId, replyToMessageId)
        );
    }

    @PostMapping(value = "/send-file/{receiverId}", consumes = {"multipart/form-data"})
    public ResponseEntity<MessageDto> sendFile(@PathVariable Long receiverId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "clientTempId", required = false) String clientTempId,
                                               @RequestParam(value = "replyToMessageId", required = false) Long replyToMessageId) {
        return ResponseEntity.ok(
                messageService.sendFileMessage(receiverId, file, clientTempId, replyToMessageId)
        );
    }
}