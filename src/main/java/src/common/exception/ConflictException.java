package src.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        this(
                "RESOURCE_CONFLICT",
                message
        );
    }

    public ConflictException(
            String code,
            String message
    ) {
        super(
                HttpStatus.CONFLICT,
                code,
                message
        );
    }
}