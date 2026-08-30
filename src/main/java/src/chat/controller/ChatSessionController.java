package src.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.chat.dto.*;
import src.chat.service.ChatDocumentContextService;
import src.chat.service.ChatSessionService;
import src.entity.User;

import src.chat.service.ChatMessageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatDocumentContextService chatDocumentContextService;

    @PostMapping("/workspaces/{workspaceId}/chat-sessions")
    public ChatSessionResponse createSession(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateChatSessionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatSessionService.createSession(
                workspaceId,
                request,
                currentUser
        );
    }

    @GetMapping("/workspaces/{workspaceId}/chat-sessions")
    public List<ChatSessionResponse> getMySessions(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatSessionService.getMySessions(
                workspaceId,
                currentUser
        );
    }

    @GetMapping("/chat-sessions/{sessionId}")
    public ChatSessionResponse getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatSessionService.getSession(
                sessionId,
                currentUser
        );
    }

    @GetMapping("/chat-sessions/{sessionId}/messages")
    public List<ChatMessageResponse> getSessionMessages(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatSessionService.getSessionMessages(
                sessionId,
                currentUser
        );
    }

    @PostMapping("/chat-sessions/{sessionId}/messages")
    public ChatAnswerResponse sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatMessageService.sendMessage(
                sessionId,
                request,
                currentUser
        );
    }

    @PostMapping(
            "/chat-sessions/{sessionId}/documents/{documentId}"
    )
    public AttachedDocumentResponse attachDocument(
            @PathVariable UUID sessionId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatDocumentContextService.attachDocument(
                sessionId,
                documentId,
                currentUser
        );
    }

    @GetMapping("/chat-sessions/{sessionId}/documents")
    public List<AttachedDocumentResponse> getAttachedDocuments(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatDocumentContextService.getAttachedDocuments(
                sessionId,
                currentUser
        );
    }

    @DeleteMapping(
            "/chat-sessions/{sessionId}/documents/{documentId}"
    )
    public void detachDocument(
            @PathVariable UUID sessionId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser
    ) {
        chatDocumentContextService.detachDocument(
                sessionId,
                documentId,
                currentUser
        );
    }

    @PatchMapping("/chat-sessions/{sessionId}")
    public ChatSessionResponse updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateChatSessionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return chatSessionService.updateSession(
                sessionId,
                request,
                currentUser
        );
    }

    @DeleteMapping("/chat-sessions/{sessionId}")
    public void deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser
    ) {
        chatSessionService.deleteSession(
                sessionId,
                currentUser
        );
    }

    @GetMapping("/chat-sessions")
    public List<ChatSessionResponse> getAllMySessions(
            @AuthenticationPrincipal User currentUser
    ) {
        return chatSessionService.getAllMySessions(
                currentUser
        );
    }
}