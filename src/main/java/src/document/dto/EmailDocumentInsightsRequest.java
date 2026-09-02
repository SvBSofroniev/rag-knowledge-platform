package src.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmailDocumentInsightsRequest(

        @NotBlank
        @Size(max = 10_000)
        String summary,

        @NotNull
        @Size(max = 6)
        List<
                @NotBlank
                @Size(max = 2_000)
                        String
                > keyPoints,

        @NotNull
        @Size(max = 8)
        List<
                @NotBlank
                @Size(max = 2_000)
                        String
                > importantFacts,

        @Size(max = 10)
        String language

) {
}