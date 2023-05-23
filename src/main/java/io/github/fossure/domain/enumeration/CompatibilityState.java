package io.github.fossure.domain.enumeration;

/**
 * The CompatibilityState enumeration.
 */
public enum CompatibilityState {
    COMPATIBLE("Compatible"),
    INCOMPATIBLE("Incompatible"),
    UNKNOWN("Unknown");

    private final String value;

    CompatibilityState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
