package src.common.language;

public enum ResponseLanguage {

    ENGLISH("English"),
    BULGARIAN("Bulgarian");

    private final String promptName;

    ResponseLanguage(
            String promptName
    ) {
        this.promptName =
                promptName;
    }

    public String promptName() {
        return promptName;
    }
}