package src.rag.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagQuestionRequest(

        @NotBlank(message = "Question cannot be empty")
        @Size(
                max = 5_000,
                message = "Question cannot exceed 5000 characters"
        )
        String question
) {
}