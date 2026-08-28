package src.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.document.dto.DocumentInsightsResponse;
import src.document.dto.GenerateDocumentInsightsRequest;
import src.document.service.DocumentInsightsService;
import src.entity.User;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentInsightsController {

    private final DocumentInsightsService
            documentInsightsService;

    @PostMapping("/{documentId}/insights")
    public DocumentInsightsResponse generateInsights(
            @PathVariable UUID documentId,
            @RequestBody(required = false)
            GenerateDocumentInsightsRequest request,
            @AuthenticationPrincipal
            User currentUser
    ) {
        String language =
                request == null
                        ? null
                        : request.language();

        return documentInsightsService
                .generateInsights(
                        documentId,
                        language,
                        currentUser
                );
    }
}
