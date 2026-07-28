package src.common.exception;

import org.springframework.http.HttpStatus;

public class DocumentProcessingException extends ApiException {

    public DocumentProcessingException(String message) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "DOCUMENT_PROCESSING_ERROR",
                message
        );
    }

    public DocumentProcessingException(
            String message,
            Throwable cause
    ) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "DOCUMENT_PROCESSING_ERROR",
                message
        );

        initCause(cause);
    }
}