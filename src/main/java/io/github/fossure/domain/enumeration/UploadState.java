package io.github.fossure.domain.enumeration;

/**
 * The UploadState enumeration.
 */
public enum UploadState {
    OK("Ok"),
    PROCESSING("Processing"),
    FAILURE("Failure");

    private final String value;

    UploadState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
