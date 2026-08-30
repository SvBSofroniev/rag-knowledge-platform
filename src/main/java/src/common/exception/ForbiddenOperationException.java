package src.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(
            String message
    ) {
        this(
                "FORBIDDEN_OPERATION",
                message
        );
    }

    public ForbiddenOperationException(
            String code,
            String message
    ) {
        super(
                HttpStatus.FORBIDDEN,
                code,
                message
        );
    }
}