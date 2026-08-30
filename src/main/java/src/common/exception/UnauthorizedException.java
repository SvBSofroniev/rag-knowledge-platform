package src.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(
            String message
    ) {
        this(
                "UNAUTHORIZED",
                message
        );
    }

    public UnauthorizedException(
            String code,
            String message
    ) {
        super(
                HttpStatus.UNAUTHORIZED,
                code,
                message
        );
    }

    public UnauthorizedException(
            String message,
            Throwable cause
    ) {
        this(
                "UNAUTHORIZED",
                message,
                cause
        );
    }

    public UnauthorizedException(
            String code,
            String message,
            Throwable cause
    ) {
        super(
                HttpStatus.UNAUTHORIZED,
                code,
                message
        );

        initCause(cause);
    }
}