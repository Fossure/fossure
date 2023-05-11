package io.github.fossure.service.exceptions;

import io.github.fossure.domain.License;

public class LicenseAlreadyExistException extends LicenseException {

    private License license = null;

    public LicenseAlreadyExistException(String message) {
        super(message);
    }

    public LicenseAlreadyExistException(String message, License license) {
        super(message);
        this.license = license;
    }

    public License getLicense() {
        return this.license;
    }
}
