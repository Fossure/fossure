package io.github.fossure.service.criteria;

import io.github.fossure.domain.Dependency;
import io.github.fossure.web.rest.v1.DependencyResource;
import org.springdoc.api.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link Dependency} entity. This class is used
 * in {@link DependencyResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /dependencies?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
public class DependencyCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LocalDateFilter addedDate;

    private BooleanFilter addedManually;

    private BooleanFilter hideForPublishing;

    private StringFilter comment;

    private LongFilter libraryId;

    private LongFilter projectId;

    private Boolean distinct;

    private StringFilter name;

    private StringFilter licensesShortIdentifier;

    private LongFilter libraryRiskId;

    private StringFilter errorLogMessage;

    private LibraryErrorLogCriteria.LogStatusFilter errorLogStatus;

    private LocalDateFilter libraryCreatedDate;

    public DependencyCriteria() {}

    public DependencyCriteria(DependencyCriteria other) {
        this.id = other.id == null ? null : other.id.copy();
        this.addedDate = other.addedDate == null ? null : other.addedDate.copy();
        this.addedManually = other.addedManually == null ? null : other.addedManually.copy();
        this.hideForPublishing = other.hideForPublishing == null ? null : other.hideForPublishing.copy();
        this.comment = other.comment == null ? null : other.comment.copy();
        this.libraryId = other.libraryId == null ? null : other.libraryId.copy();
        this.projectId = other.projectId == null ? null : other.projectId.copy();
        this.distinct = other.distinct;
        this.name = other.name == null ? null : other.name.copy();
        this.licensesShortIdentifier = other.licensesShortIdentifier == null ? null : other.licensesShortIdentifier.copy();
        this.libraryRiskId = other.libraryRiskId == null ? null : other.libraryRiskId.copy();
        this.errorLogMessage = other.errorLogMessage == null ? null : other.errorLogMessage.copy();
        this.errorLogStatus = other.errorLogStatus == null ? null : other.errorLogStatus.copy();
        this.libraryCreatedDate = other.libraryCreatedDate == null ? null : other.libraryCreatedDate.copy();
    }

    @Override
    public DependencyCriteria copy() {
        return new DependencyCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public LongFilter id() {
        if (id == null) {
            id = new LongFilter();
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public LocalDateFilter getAddedDate() {
        return addedDate;
    }

    public LocalDateFilter addedDate() {
        if (addedDate == null) {
            addedDate = new LocalDateFilter();
        }
        return addedDate;
    }

    public void setAddedDate(LocalDateFilter addedDate) {
        this.addedDate = addedDate;
    }

    public BooleanFilter getAddedManually() {
        return addedManually;
    }

    public BooleanFilter addedManually() {
        if (addedManually == null) {
            addedManually = new BooleanFilter();
        }
        return addedManually;
    }

    public void setAddedManually(BooleanFilter addedManually) {
        this.addedManually = addedManually;
    }

    public BooleanFilter getHideForPublishing() {
        return hideForPublishing;
    }

    public BooleanFilter hideForPublishing() {
        if (hideForPublishing == null) {
            hideForPublishing = new BooleanFilter();
        }
        return hideForPublishing;
    }

    public void setHideForPublishing(BooleanFilter hideForPublishing) {
        this.hideForPublishing = hideForPublishing;
    }

    public StringFilter getComment() {
        return comment;
    }

    public StringFilter comment() {
        if (comment == null) {
            comment = new StringFilter();
        }
        return comment;
    }

    public void setComment(StringFilter comment) {
        this.comment = comment;
    }

    public LongFilter getLibraryId() {
        return libraryId;
    }

    public LongFilter libraryId() {
        if (libraryId == null) {
            libraryId = new LongFilter();
        }
        return libraryId;
    }

    public void setLibraryId(LongFilter libraryId) {
        this.libraryId = libraryId;
    }

    public LongFilter getProjectId() {
        return projectId;
    }

    public void setProjectId(LongFilter projectId) {
        this.projectId = projectId;
    }

    public LongFilter projectId() {
        if (projectId == null) {
            projectId = new LongFilter();
        }
        return projectId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    public StringFilter getName() {
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    public StringFilter getLicensesShortIdentifier() {
        return licensesShortIdentifier;
    }

    public void setLicensesShortIdentifier(StringFilter licensesShortIdentifier) {
        this.licensesShortIdentifier = licensesShortIdentifier;
    }

    public StringFilter licensesShortIdentifier() {
        if (licensesShortIdentifier == null) {
            licensesShortIdentifier = new StringFilter();
        }
        return licensesShortIdentifier;
    }

    public LongFilter getLibraryRiskId() {
        return libraryRiskId;
    }

    public void setLibraryRiskId(LongFilter libraryRiskId) {
        this.libraryRiskId = libraryRiskId;
    }

    public LongFilter licenseRiskId() {
        if (libraryRiskId == null) {
            libraryRiskId = new LongFilter();
        }
        return libraryRiskId;
    }

    public StringFilter getErrorLogMessage() {
        return errorLogMessage;
    }

    public void setErrorLogMessage(StringFilter errorLogMessage) {
        this.errorLogMessage = errorLogMessage;
    }

    public StringFilter errorLogMessage() {
        if (errorLogMessage == null) {
            errorLogMessage = new StringFilter();
        }
        return errorLogMessage;
    }

    public LibraryErrorLogCriteria.LogStatusFilter getErrorLogStatus() {
        return errorLogStatus;
    }

    public void setErrorLogStatus(LibraryErrorLogCriteria.LogStatusFilter errorLogStatus) {
        this.errorLogStatus = errorLogStatus;
    }

    public LibraryErrorLogCriteria.LogStatusFilter errorLogStatus() {
        if (errorLogStatus == null) {
            errorLogStatus = new LibraryErrorLogCriteria.LogStatusFilter();
        }
        return errorLogStatus;
    }

    public LocalDateFilter getLibraryCreatedDate() {
        return libraryCreatedDate;
    }

    public void setLibraryCreatedDate(LocalDateFilter libraryCreatedDate) {
        this.libraryCreatedDate = libraryCreatedDate;
    }

    public LocalDateFilter libraryCreatedDate() {
        if (libraryCreatedDate == null) {
            libraryCreatedDate = new LocalDateFilter();
        }
        return libraryCreatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DependencyCriteria that = (DependencyCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(addedDate, that.addedDate) &&
            Objects.equals(addedManually, that.addedManually) &&
            Objects.equals(hideForPublishing, that.hideForPublishing) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(libraryId, that.libraryId) &&
            Objects.equals(projectId, that.projectId) &&
            Objects.equals(distinct, that.distinct) &&
            Objects.equals(name, that.name) &&
            Objects.equals(licensesShortIdentifier, that.licensesShortIdentifier) &&
            Objects.equals(libraryRiskId, that.libraryRiskId) &&
            Objects.equals(errorLogMessage, that.errorLogMessage) &&
            Objects.equals(errorLogStatus, that.errorLogStatus) &&
            Objects.equals(libraryCreatedDate, that.libraryCreatedDate)

        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            addedDate,
            addedManually,
            hideForPublishing,
            comment,
            libraryId,
            projectId,
            distinct,
            name,
            licensesShortIdentifier,
            libraryRiskId,
            errorLogMessage,
            errorLogStatus,
            libraryCreatedDate
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DependencyCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (addedDate != null ? "addedDate=" + addedDate + ", " : "") +
            (addedManually != null ? "addedManually=" + addedManually + ", " : "") +
            (hideForPublishing != null ? "hideForPublishing=" + hideForPublishing + ", " : "") +
            (comment != null ? "comment=" + comment + ", " : "") +
            (libraryId != null ? "libraryId=" + libraryId + ", " : "") +
            (projectId != null ? "projectId=" + projectId + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            (name != null ? "name=" + name + ", " : "") +
            (licensesShortIdentifier != null ? "licensesShortIdentifier=" + licensesShortIdentifier + ", " : "") +
            (libraryRiskId != null ? "libraryRiskId=" + libraryRiskId + ", " : "") +
            (errorLogMessage != null ? "errorLogMessage=" + errorLogMessage + ", " : "") +
            (errorLogStatus != null ? "errorLogStatus=" + errorLogStatus + ", " : "") +
            (libraryCreatedDate != null ? "libraryCreatedDate=" + libraryCreatedDate + ", " : "") +
            "}";
    }
}
