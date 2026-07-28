package src.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT",
                message
        );
    }
}