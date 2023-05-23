package io.github.fossure.domain.enumeration;

/**
 * The EntityUploadChoice enumeration.
 */
public enum EntityUploadChoice {
    PROJECT("Project"),
    LIBRARY("Library"),
    LICENSE("License");

    private final String value;

    EntityUploadChoice(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
