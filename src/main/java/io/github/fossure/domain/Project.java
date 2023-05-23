package io.github.fossure.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.github.fossure.domain.enumeration.UploadState;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

/**
 * A Project.
 */
@Entity
@Table(name = "project")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "label", nullable = false)
    private String label;

    @NotNull
    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "created_date")
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "last_updated_date")
    private LocalDate lastUpdatedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_state")
    private UploadState uploadState = UploadState.OK;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "disclaimer")
    private String disclaimer;

    @Column(name = "delivered")
    private Boolean delivered = false;

    @Column(name = "delivered_date")
    private Instant deliveredDate = null;

    @Size(max = 2048)
    @Column(name = "contact", length = 2048)
    private String contact;

    @Size(max = 4096)
    @Column(name = "comment", length = 4096)
    private String comment;

    @Size(max = 2048)
    @Column(name = "upload_filter", length = 2048)
    private String uploadFilter;

    @JsonIgnoreProperties(value = {
        "createdDate", "lastUpdatedDate", "uploadState", "disclaimer", "delivered",
        "deliveredDate", "contact", "comment", "uploadFilter", "previousProject", "libraries"
    },
        allowSetters = true)
    @OneToOne
    @JoinColumn(unique = true)
    private Project previousProject;

    @OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"library", "project"}, allowSetters = true)
    private Set<Dependency> libraries = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project id(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public Project name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Project label(String label) {
        this.label = label;
        return this;
    }

    public String getVersion() {
        return this.version;
    }

    public Project version(String version) {
        this.version = version;
        return this;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getCreatedDate() {
        return this.createdDate;
    }

    public Project createdDate(LocalDate createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDate getLastUpdatedDate() {
        return this.lastUpdatedDate;
    }

    public Project lastUpdatedDate(LocalDate lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
        return this;
    }

    public void setLastUpdatedDate(LocalDate lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public UploadState getUploadState() {
        return this.uploadState;
    }

    public Project uploadState(UploadState uploadState) {
        this.uploadState = uploadState;
        return this;
    }

    public void setUploadState(UploadState uploadState) {
        this.uploadState = uploadState;
    }

    public String getDisclaimer() {
        return this.disclaimer;
    }

    public Project disclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
        return this;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public Boolean getDelivered() {
        return delivered;
    }

    public Project delivered(Boolean delivered) {
        this.delivered = delivered;
        return this;
    }

    public void setDelivered(Boolean delivered) {
        this.delivered = delivered;
    }

    public Instant getDeliveredDate() {
        return deliveredDate;
    }

    public Project deliveredDate(Instant deliveredDate) {
        this.deliveredDate = deliveredDate;
        return this;
    }

    public void setDeliveredDate(Instant deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public String getContact() {
        return contact;
    }

    public Project contact(String contact) {
        this.contact = contact;
        return this;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getComment() {
        return comment;
    }

    public Project comment(String comment) {
        this.comment = comment;
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Project getPreviousProject() {
        return previousProject;
    }

    public void setPreviousProject(Project previousProject) {
        this.previousProject = previousProject;
    }

    public Project previousProject(Project previousProject) {
        this.previousProject = previousProject;
        return this;
    }

    public String getUploadFilter() {
        return uploadFilter;
    }

    public Project uploadFilter(String uploadFilter) {
        this.uploadFilter = uploadFilter;
        return this;
    }

    public void setUploadFilter(String uploadFilter) {
        this.uploadFilter = uploadFilter;
    }

    public Set<Dependency> getLibraries() {
        return this.libraries;
    }

    public void setLibraries(Set<Dependency> dependencies) {
        if (this.libraries != null) {
            this.libraries.forEach(i -> i.setProject(null));
        }
        if (dependencies != null) {
            dependencies.forEach(i -> i.setProject(this));
        }
        this.libraries = dependencies;
    }

    public Project libraries(Set<Dependency> dependencies) {
        this.setLibraries(dependencies);
        return this;
    }

    public Project addLibrary(Dependency dependency) {
        this.libraries.add(dependency);
        dependency.setProject(this);
        return this;
    }

    public Project removeLibrary(Dependency dependency) {
        this.libraries.remove(dependency);
        dependency.setProject(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project)) {
            return false;
        }
        return id != null && id.equals(((Project) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Project{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", label='" + getLabel() + "'" +
            ", version='" + getVersion() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastUpdatedDate='" + getLastUpdatedDate() + "'" +
            ", uploadState='" + getUploadState() + "'" +
            ", disclaimer='" + StringUtils.abbreviate(getDisclaimer(), 20) + "'" +
            ", delivered='" + getDelivered() + "'" +
            ", deliveredDate='" + getDeliveredDate() + "'" +
            ", contact='" + StringUtils.abbreviate(getContact(), 20) + "'" +
            ", comment='" + StringUtils.abbreviate(getComment(), 20) + "'" +
            ", uploadFilter='" + StringUtils.abbreviate(getUploadFilter(), 20) + "'" +
            "}";
    }
}
