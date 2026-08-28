package src.document.dto;

import java.util.List;

public record DocumentInsightsResponse(
        String summary,
        List<String> keyPoints,
        List<String> importantFacts
) {
}