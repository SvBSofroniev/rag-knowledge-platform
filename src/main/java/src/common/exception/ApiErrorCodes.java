package src.common.exception;

public final class ApiErrorCodes {

    private ApiErrorCodes() {
    }

    /*
     * =========================================================
     * AUTHENTICATION
     * =========================================================
     */

    public static final String EMAIL_ALREADY_EXISTS =
            "EMAIL_ALREADY_EXISTS";

    public static final String USERNAME_ALREADY_EXISTS =
            "USERNAME_ALREADY_EXISTS";

    public static final String INVALID_CREDENTIALS =
            "INVALID_CREDENTIALS";

    public static final String ACCOUNT_DISABLED =
            "ACCOUNT_DISABLED";

    public static final String ACCOUNT_LOCKED =
            "ACCOUNT_LOCKED";

    public static final String INVALID_REFRESH_TOKEN =
            "INVALID_REFRESH_TOKEN";

    public static final String REFRESH_TOKEN_EXPIRED =
            "REFRESH_TOKEN_EXPIRED";

    public static final String REFRESH_TOKEN_REVOKED =
            "REFRESH_TOKEN_REVOKED";


    /*
     * =========================================================
     * USER / PROFILE
     * =========================================================
     */

    public static final String USER_NOT_FOUND =
            "USER_NOT_FOUND";

    public static final String CURRENT_PASSWORD_INCORRECT =
            "CURRENT_PASSWORD_INCORRECT";

    public static final String PASSWORDS_DO_NOT_MATCH =
            "PASSWORDS_DO_NOT_MATCH";

    public static final String PASSWORD_MUST_BE_DIFFERENT =
            "PASSWORD_MUST_BE_DIFFERENT";

    public static final String USER_SEARCH_QUERY_TOO_SHORT =
            "USER_SEARCH_QUERY_TOO_SHORT";


    /*
     * =========================================================
     * WORKSPACE
     * =========================================================
     */

    public static final String WORKSPACE_NOT_FOUND =
            "WORKSPACE_NOT_FOUND";

    public static final String WORKSPACE_MEMBER_NOT_FOUND =
            "WORKSPACE_MEMBER_NOT_FOUND";

    public static final String WORKSPACE_MEMBER_ALREADY_EXISTS =
            "WORKSPACE_MEMBER_ALREADY_EXISTS";

    public static final String WORKSPACE_ADMIN_REQUIRED =
            "WORKSPACE_ADMIN_REQUIRED";

    public static final String WORKSPACE_OWNER_REQUIRED =
            "WORKSPACE_OWNER_REQUIRED";

    public static final String WORKSPACE_OWNER_ROLE_IMMUTABLE =
            "WORKSPACE_OWNER_ROLE_IMMUTABLE";

    public static final String WORKSPACE_OWNER_ASSIGNMENT_FORBIDDEN =
            "WORKSPACE_OWNER_ASSIGNMENT_FORBIDDEN";

    public static final String WORKSPACE_ROLE_UNCHANGED =
            "WORKSPACE_ROLE_UNCHANGED";

    public static final String ADMIN_CANNOT_REMOVE_ADMIN =
            "ADMIN_CANNOT_REMOVE_ADMIN";

    public static final String WORKSPACE_NAME_REQUIRED =
            "WORKSPACE_NAME_REQUIRED";


    /*
     * =========================================================
     * DOCUMENT
     * =========================================================
     */

    public static final String DOCUMENT_NOT_FOUND =
            "DOCUMENT_NOT_FOUND";

    public static final String DOCUMENT_NOT_READY =
            "DOCUMENT_NOT_READY";

    public static final String DOCUMENT_ALREADY_PROCESSING =
            "DOCUMENT_ALREADY_PROCESSING";

    public static final String DOCUMENT_PROCESSING_CONFLICT =
            "DOCUMENT_PROCESSING_CONFLICT";

    public static final String DOCUMENT_CONTENT_EMPTY =
            "DOCUMENT_CONTENT_EMPTY";

    public static final String DOCUMENT_INSIGHTS_TOO_LARGE =
            "DOCUMENT_INSIGHTS_TOO_LARGE";

    public static final String DOCUMENT_FILE_EMPTY =
            "DOCUMENT_FILE_EMPTY";

    public static final String DOCUMENT_FILENAME_MISSING =
            "DOCUMENT_FILENAME_MISSING";

    public static final String DOCUMENT_FILENAME_INVALID =
            "DOCUMENT_FILENAME_INVALID";

    public static final String DOCUMENT_EXTENSION_REQUIRED =
            "DOCUMENT_EXTENSION_REQUIRED";

    public static final String DOCUMENT_EXTENSION_INVALID =
            "DOCUMENT_EXTENSION_INVALID";

    public static final String UNSUPPORTED_DOCUMENT_TYPE =
            "UNSUPPORTED_DOCUMENT_TYPE";

    public static final String DOCUMENT_ALREADY_ATTACHED =
            "DOCUMENT_ALREADY_ATTACHED";

    public static final String DOCUMENT_NOT_ATTACHED =
            "DOCUMENT_NOT_ATTACHED";

    public static final String DOCUMENT_ALREADY_PROCESSED =
            "DOCUMENT_ALREADY_PROCESSED";

    public static final String DOCUMENT_REQUIRED =
            "DOCUMENT_REQUIRED";

    /*
     * =========================================================
     * CHAT
     * =========================================================
     */

    public static final String CHAT_SESSION_NOT_FOUND =
            "CHAT_SESSION_NOT_FOUND";

    public static final String CHAT_MESSAGE_REQUEST_REQUIRED =
            "CHAT_MESSAGE_REQUEST_REQUIRED";

    public static final String CHAT_MESSAGE_REQUIRED =
            "CHAT_MESSAGE_REQUIRED";

    public static final String CHAT_MESSAGE_TOO_LONG =
            "CHAT_MESSAGE_TOO_LONG";

    public static final String CHAT_SESSION_UPDATE_REQUIRED =
            "CHAT_SESSION_UPDATE_REQUIRED";

    public static final String CHAT_TITLE_REQUIRED =
            "CHAT_TITLE_REQUIRED";

    public static final String CHAT_TITLE_TOO_LONG =
            "CHAT_TITLE_TOO_LONG";


    /*
     * =========================================================
     * RAG / SEARCH
     * =========================================================
     */

    public static final String SEARCH_QUERY_REQUIRED =
            "SEARCH_QUERY_REQUIRED";

    public static final String SEARCH_QUERY_TOO_LONG =
            "SEARCH_QUERY_TOO_LONG";

    public static final String SEARCH_LIMIT_INVALID =
            "SEARCH_LIMIT_INVALID";

    public static final String CONTEXT_LIMIT_INVALID =
            "CONTEXT_LIMIT_INVALID";

    public static final String DOCUMENT_SELECTION_REQUIRED =
            "DOCUMENT_SELECTION_REQUIRED";

    public static final String QUESTION_REQUIRED =
            "QUESTION_REQUIRED";

    public static final String QUESTION_TOO_LONG =
            "QUESTION_TOO_LONG";


    /*
     * =========================================================
     * EMBEDDINGS
     * =========================================================
     */

    public static final String EMBEDDING_INPUT_REQUIRED =
            "EMBEDDING_INPUT_REQUIRED";

    public static final String EMBEDDING_INPUT_TOO_LONG =
            "EMBEDDING_INPUT_TOO_LONG";


    /*
     * =========================================================
     * FILE STORAGE
     * =========================================================
     */

    public static final String STORED_FILE_PATH_REQUIRED =
            "STORED_FILE_PATH_REQUIRED";

    public static final String STORED_FILE_PATH_INVALID =
            "STORED_FILE_PATH_INVALID";

    public static final String INVALID_FILE_STORAGE_PATH =
            "INVALID_FILE_STORAGE_PATH";
}