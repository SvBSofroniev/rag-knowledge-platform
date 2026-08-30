package src.common.exception;

import org.springframework.http.HttpStatus;

public class AiModelResponseException extends ApiException {

    public AiModelResponseException(
            String message
    ) {
        super(
                HttpStatus.BAD_GATEWAY,
                "INVALID_AI_MODEL_RESPONSE",
                message
        );
    }

    public AiModelResponseException(
            String message,
            Throwable cause
    ) {
        super(
                HttpStatus.BAD_GATEWAY,
                "INVALID_AI_MODEL_RESPONSE",
                message,
                cause
        );
    }

}