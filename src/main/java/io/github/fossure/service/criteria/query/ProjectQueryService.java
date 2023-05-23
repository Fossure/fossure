package io.github.fossure.service.criteria.query;

import java.util.List;
import javax.persistence.criteria.JoinType;

import io.github.fossure.domain.Dependency_;
import io.github.fossure.domain.Project_;
import io.github.fossure.domain.Project;
import io.github.fossure.repository.ProjectRepository;
import io.github.fossure.service.criteria.ProjectCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Project} entities in the database.
 * The main input is a {@link ProjectCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link Project} or a {@link Page} of {@link Project} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ProjectQueryService extends QueryService<Project> {

    private final Logger log = LoggerFactory.getLogger(ProjectQueryService.class);

    private final ProjectRepository projectRepository;

    public ProjectQueryService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Return a {@link List} of {@link Project} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<Project> findByCriteria(ProjectCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Project> specification = createSpecification(criteria);
        return projectRepository.findAll(specification);
    }

    /**
     * Return a {@link Page} of {@link Project} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<Project> findByCriteria(ProjectCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Project> specification = createSpecification(criteria);
        return projectRepository.findAll(specification, page);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ProjectCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Project> specification = createSpecification(criteria);
        return projectRepository.count(specification);
    }

    /**
     * Function to convert {@link ProjectCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Project> createSpecification(ProjectCriteria criteria) {
        Specification<Project> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), Project_.id));
            }
            if (criteria.getName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getName(), Project_.name));
            }
            if (criteria.getLabel() != null) {
                specification = specification.and(buildStringSpecification(criteria.getLabel(), Project_.label));
            }
            if (criteria.getVersion() != null) {
                specification = specification.and(buildStringSpecification(criteria.getVersion(), Project_.version));
            }
            if (criteria.getCreatedDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getCreatedDate(), Project_.createdDate));
            }
            if (criteria.getLastUpdatedDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getLastUpdatedDate(), Project_.lastUpdatedDate));
            }
            if (criteria.getUploadState() != null) {
                specification = specification.and(buildSpecification(criteria.getUploadState(), Project_.uploadState));
            }
            if (criteria.getDelivered() != null) {
                specification = specification.and(buildSpecification(criteria.getDelivered(), Project_.delivered));
            }
            if (criteria.getDeliveredDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getDeliveredDate(), Project_.deliveredDate));
            }
            if (criteria.getContact() != null) {
                specification = specification.and(buildStringSpecification(criteria.getContact(), Project_.contact));
            }
            if (criteria.getComment() != null) {
                specification = specification.and(buildStringSpecification(criteria.getComment(), Project_.comment));
            }
            if (criteria.getUploadFilter() != null) {
                specification = specification.and(buildStringSpecification(criteria.getUploadFilter(), Project_.uploadFilter));
            }
            if (criteria.getPreviousProjectId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getPreviousProjectId(),
                            root -> root.join(Project_.previousProject, JoinType.LEFT).get(Project_.id)
                        )
                    );
            }
            if (criteria.getLibraryId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLibraryId(),
                            root -> root.join(Project_.libraries, JoinType.LEFT).get(Dependency_.id)
                        )
                    );
            }
        }
        return specification;
    }
}
