package io.github.fossure.service.exceptions;

import io.github.fossure.domain.Library;

public class LibraryAlreadyExistException extends LibraryException {

    public LibraryAlreadyExistException(String message) {
        super(message);
    }

    public LibraryAlreadyExistException(String message, Library library) {
        super(message, library);
    }
}
