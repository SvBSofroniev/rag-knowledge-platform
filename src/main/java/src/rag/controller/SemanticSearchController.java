package src.rag.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.entity.User;
import src.rag.dto.SemanticSearchResponse;
import src.rag.service.SemanticSearchService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping
    public List<SemanticSearchResponse> search(
            @PathVariable UUID workspaceId,
            @RequestParam("query") String query,
            @RequestParam(
                    name = "limit",
                    required = false
            ) Integer limit,
            @AuthenticationPrincipal User currentUser
    ) {
        return semanticSearchService.search(
                workspaceId,
                query,
                limit,
                currentUser
        );
    }
}