package io.github.fossure.service.exceptions;

import io.github.fossure.domain.Project;

public class ProjectException extends Exception {

    private static final long serialVersionUID = 1L;
    private Project project;

    public ProjectException(String message) {
        super(message);
    }

    public ProjectException(String message, Project project) {
        super(message);
        this.project = project;
    }

    public Project getProject() {
        return this.project;
    }
}
