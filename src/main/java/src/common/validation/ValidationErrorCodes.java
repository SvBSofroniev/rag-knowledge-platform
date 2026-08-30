package src.common.validation;

public final class ValidationErrorCodes {

    private ValidationErrorCodes() {
    }

    public static final String USERNAME_REQUIRED =
            "USERNAME_REQUIRED";

    public static final String USERNAME_LENGTH =
            "USERNAME_LENGTH";

    public static final String FIRST_NAME_REQUIRED =
            "FIRST_NAME_REQUIRED";

    public static final String FIRST_NAME_LENGTH =
            "FIRST_NAME_LENGTH";

    public static final String LAST_NAME_REQUIRED =
            "LAST_NAME_REQUIRED";

    public static final String LAST_NAME_LENGTH =
            "LAST_NAME_LENGTH";

    public static final String EMAIL_REQUIRED =
            "EMAIL_REQUIRED";

    public static final String EMAIL_INVALID =
            "EMAIL_INVALID";

    public static final String EMAIL_LENGTH =
            "EMAIL_LENGTH";

    public static final String DATE_OF_BIRTH_PAST =
            "DATE_OF_BIRTH_PAST";

    public static final String PASSWORD_REQUIRED =
            "PASSWORD_REQUIRED";

    public static final String PASSWORD_LENGTH =
            "PASSWORD_LENGTH";

    public static final String CURRENT_PASSWORD_REQUIRED =
            "CURRENT_PASSWORD_REQUIRED";

    public static final String NEW_PASSWORD_REQUIRED =
            "NEW_PASSWORD_REQUIRED";

    public static final String NEW_PASSWORD_LENGTH =
            "NEW_PASSWORD_LENGTH";

    public static final String PASSWORD_CONFIRMATION_REQUIRED =
            "PASSWORD_CONFIRMATION_REQUIRED";
}
