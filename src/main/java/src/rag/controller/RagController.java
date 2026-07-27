package src.rag.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.entity.User;
import src.rag.dto.RagAnswerResponse;
import src.rag.dto.RagQuestionRequest;
import src.rag.service.RagService;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/ask")
    public RagAnswerResponse askQuestion(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody RagQuestionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ragService.answer(
                workspaceId,
                request.question(),
                currentUser
        );
    }
}