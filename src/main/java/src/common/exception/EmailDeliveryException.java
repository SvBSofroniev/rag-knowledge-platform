package src.common.exception;

import org.springframework.http.HttpStatus;

public class EmailDeliveryException
        extends ApiException {

    public EmailDeliveryException(
            String message,
            Throwable cause
    ) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorCodes.EMAIL_DELIVERY_FAILED,
                message,
                cause
        );
    }
}