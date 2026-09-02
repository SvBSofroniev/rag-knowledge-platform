package src.document.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.document.dto.EmailDocumentInsightsRequest;
import src.document.service.DocumentInsightsEmailService;
import src.entity.User;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentInsightsEmailController {

    private final DocumentInsightsEmailService
            documentInsightsEmailService;

    @PostMapping(
            "/{documentId}/insights/email"
    )
    @ResponseStatus(
            HttpStatus.NO_CONTENT
    )
    public void emailInsights(
            @PathVariable
            UUID documentId,

            @Valid
            @RequestBody
            EmailDocumentInsightsRequest request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        documentInsightsEmailService
                .sendInsights(
                        documentId,
                        request,
                        currentUser
                );
    }
}