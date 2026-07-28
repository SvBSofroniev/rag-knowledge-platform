package src.common.exception;

import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends ApiException {

    public AiServiceUnavailableException(String message) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_SERVICE_UNAVAILABLE",
                message
        );
    }

    public AiServiceUnavailableException(
            String message,
            Throwable cause
    ) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_SERVICE_UNAVAILABLE",
                message
        );

        initCause(cause);
    }
}