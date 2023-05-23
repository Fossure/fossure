package io.github.fossure.service.criteria;

import io.github.fossure.domain.Project;
import io.github.fossure.domain.enumeration.UploadState;
import io.github.fossure.web.rest.v1.ProjectResource;
import org.springdoc.api.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link Project} entity. This class is used
 * in {@link ProjectResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /projects?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
public class ProjectCriteria implements Serializable, Criteria {

    /**
     * Class for filtering UploadState
     */
    public static class UploadStateFilter extends Filter<UploadState> {

        public UploadStateFilter() {}

        public UploadStateFilter(UploadStateFilter filter) {
            super(filter);
        }

        @Override
        public UploadStateFilter copy() {
            return new UploadStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter name;

    private StringFilter label;

    private StringFilter version;

    private LocalDateFilter createdDate;

    private LocalDateFilter lastUpdatedDate;

    private UploadStateFilter uploadState;

    private BooleanFilter delivered;

    private InstantFilter deliveredDate;

    private StringFilter contact;

    private StringFilter comment;

    private LongFilter previousProjectId;

    private StringFilter uploadFilter;

    private LongFilter libraryId;

    private Boolean distinct;

    public ProjectCriteria() {}

    public ProjectCriteria(ProjectCriteria other) {
        this.id = other.id == null ? null : other.id.copy();
        this.name = other.name == null ? null : other.name.copy();
        this.label = other.label == null ? null : other.label.copy();
        this.version = other.version == null ? null : other.version.copy();
        this.createdDate = other.createdDate == null ? null : other.createdDate.copy();
        this.lastUpdatedDate = other.lastUpdatedDate == null ? null : other.lastUpdatedDate.copy();
        this.uploadState = other.uploadState == null ? null : other.uploadState.copy();
        this.delivered = other.delivered == null ? null : other.delivered.copy();
        this.deliveredDate = other.deliveredDate == null ? null : other.deliveredDate.copy();
        this.contact = other.contact == null ? null : other.contact.copy();
        this.comment = other.comment == null ? null : other.comment.copy();
        this.previousProjectId = other.previousProjectId == null ? null : other.previousProjectId.copy();
        this.uploadFilter = other.uploadFilter == null ? null : other.uploadFilter.copy();
        this.libraryId = other.libraryId == null ? null : other.libraryId.copy();
        this.distinct = other.distinct;
    }

    @Override
    public ProjectCriteria copy() {
        return new ProjectCriteria(this);
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

    public StringFilter getName() {
        return name;
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter getLabel() {
        return label;
    }

    public void setLabel(StringFilter label) {
        this.label = label;
    }

    public StringFilter label() {
        if (label == null) {
            label = new StringFilter();
        }
        return label;
    }

    public StringFilter getVersion() {
        return version;
    }

    public StringFilter version() {
        if (version == null) {
            version = new StringFilter();
        }
        return version;
    }

    public void setVersion(StringFilter version) {
        this.version = version;
    }

    public LocalDateFilter getCreatedDate() {
        return createdDate;
    }

    public LocalDateFilter createdDate() {
        if (createdDate == null) {
            createdDate = new LocalDateFilter();
        }
        return createdDate;
    }

    public void setCreatedDate(LocalDateFilter createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateFilter getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public LocalDateFilter lastUpdatedDate() {
        if (lastUpdatedDate == null) {
            lastUpdatedDate = new LocalDateFilter();
        }
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateFilter lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public UploadStateFilter getUploadState() {
        return uploadState;
    }

    public UploadStateFilter uploadState() {
        if (uploadState == null) {
            uploadState = new UploadStateFilter();
        }
        return uploadState;
    }

    public void setUploadState(UploadStateFilter uploadState) {
        this.uploadState = uploadState;
    }

    public BooleanFilter getDelivered() {
        return delivered;
    }

    public BooleanFilter delivered() {
        if (delivered == null) {
            delivered = new BooleanFilter();
        }
        return delivered;
    }

    public void setDelivered(BooleanFilter delivered) {
        this.delivered = delivered;
    }

    public InstantFilter getDeliveredDate() {
        return deliveredDate;
    }

    public InstantFilter deliveredDate() {
        if (deliveredDate == null) {
            deliveredDate = new InstantFilter();
        }
        return deliveredDate;
    }

    public void setDeliveredDate(InstantFilter deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public StringFilter getContact() {
        return contact;
    }

    public StringFilter contact() {
        if (contact == null) {
            contact = new StringFilter();
        }
        return contact;
    }

    public void setContact(StringFilter contact) {
        this.contact = contact;
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

    public LongFilter getPreviousProjectId() {
        return previousProjectId;
    }

    public void setPreviousProjectId(LongFilter previousProjectId) {
        this.previousProjectId = previousProjectId;
    }

    public LongFilter previousProjectId() {
        if (previousProjectId == null) {
            previousProjectId = new LongFilter();
        }
        return previousProjectId;
    }

    public StringFilter getUploadFilter() {
        return uploadFilter;
    }

    public StringFilter uploadFilter() {
        if (uploadFilter == null) {
            uploadFilter = new StringFilter();
        }
        return uploadFilter;
    }

    public void setUploadFilter(StringFilter uploadFilter) {
        this.uploadFilter = uploadFilter;
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

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProjectCriteria that = (ProjectCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(label, that.label) &&
            Objects.equals(version, that.version) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(lastUpdatedDate, that.lastUpdatedDate) &&
            Objects.equals(uploadState, that.uploadState) &&
            Objects.equals(delivered, that.delivered) &&
            Objects.equals(deliveredDate, that.deliveredDate) &&
            Objects.equals(contact, that.contact) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(previousProjectId, that.previousProjectId) &&
            Objects.equals(uploadFilter, that.uploadFilter) &&
            Objects.equals(libraryId, that.libraryId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            name,
            label,
            version,
            createdDate,
            lastUpdatedDate,
            uploadState,
            delivered,
            deliveredDate,
            contact,
            comment,
            previousProjectId,
            uploadFilter,
            libraryId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProjectCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (name != null ? "name=" + name + ", " : "") +
            (label != null ? "label=" + label + ", " : "") +
            (version != null ? "version=" + version + ", " : "") +
            (createdDate != null ? "createdDate=" + createdDate + ", " : "") +
            (lastUpdatedDate != null ? "lastUpdatedDate=" + lastUpdatedDate + ", " : "") +
            (uploadState != null ? "uploadState=" + uploadState + ", " : "") +
            (delivered != null ? "delivered=" + delivered + ", " : "") +
            (deliveredDate != null ? "deliveredDate=" + deliveredDate + ", " : "") +
            (contact != null ? "contact=" + contact + ", " : "") +
            (comment != null ? "comment=" + comment + ", " : "") +
            (previousProjectId != null ? "previousProjectId=" + previousProjectId + ", " : "") +
            (uploadFilter != null ? "uploadFilter=" + uploadFilter + ", " : "") +
            (libraryId != null ? "libraryId=" + libraryId + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
