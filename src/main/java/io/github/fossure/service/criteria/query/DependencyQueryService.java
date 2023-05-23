package io.github.fossure.service.criteria.query;

import io.github.fossure.domain.*;
import io.github.fossure.repository.DependencyRepository;
import io.github.fossure.service.criteria.DependencyCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

import javax.persistence.criteria.JoinType;
import java.util.List;

/**
 * Service for executing complex queries for {@link Dependency} entities in the database.
 * The main input is a {@link DependencyCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link Dependency} or a {@link Page} of {@link Dependency} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DependencyQueryService extends QueryService<Dependency> {

    private final Logger log = LoggerFactory.getLogger(DependencyQueryService.class);

    private final DependencyRepository dependencyRepository;

    public DependencyQueryService(DependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    /**
     * Return a {@link List} of {@link Dependency} which matches the criteria from the database.
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<Dependency> findByCriteria(DependencyCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Dependency> specification = createSpecification(criteria);
        return dependencyRepository.findAll(specification);
    }

    /**
     * Return a {@link Page} of {@link Dependency} which matches the criteria from the database.
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page     The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<Dependency> findByCriteria(DependencyCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Dependency> specification = createSpecification(criteria);
        return dependencyRepository.findAll(specification, page);
    }

    /**
     * Return the number of matching entities in the database.
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DependencyCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Dependency> specification = createSpecification(criteria);
        return dependencyRepository.count(specification);
    }

    /**
     * Function to convert {@link DependencyCriteria} to a {@link Specification}
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Dependency> createSpecification(DependencyCriteria criteria) {
        Specification<Dependency> specification = Specification.where(null);

        /*
        Query returns duplicate rows for many to many. Solution is to initialise the specification with a distinct query
        !!! Solution is not working because distinct and orderBy is cannot work together, if the select statement
        doesn't contain the field to order by !!!
        */
        /*Specification<Dependency> specification = (root, query, cb) -> {
            query.distinct(true);
            root.fetch(Dependency_.library, JoinType.LEFT);
            return null;
        };*/

        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), Dependency_.id));
            }
            if (criteria.getAddedDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getAddedDate(), Dependency_.addedDate));
            }
            if (criteria.getAddedManually() != null) {
                specification = specification.and(buildSpecification(criteria.getAddedManually(), Dependency_.addedManually));
            }
            if (criteria.getHideForPublishing() != null) {
                specification =
                    specification.and(buildSpecification(criteria.getHideForPublishing(), Dependency_.hideForPublishing));
            }
            if (criteria.getComment() != null) {
                specification = specification.and(buildSpecification(criteria.getComment(), Dependency_.comment));
            }
            if (criteria.getLibraryId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLibraryId(),
                            root -> root.join(Dependency_.library, JoinType.LEFT).get(Library_.id)
                        )
                    );
            }
            if (criteria.getProjectId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getProjectId(),
                            root -> root.join(Dependency_.project, JoinType.LEFT).get(Project_.id)
                        )
                    );
            }
            if (criteria.getArtifactId() != null) {
                if (criteria.getArtifactId().getContains() != null) {
                    String artifactId = "%" + criteria.getArtifactId().getContains() + "%";
                    specification =
                        specification.and((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.or(
                                criteriaBuilder.like(
                                    root.join(Dependency_.library, JoinType.LEFT).get(Library_.artifactId),
                                    artifactId
                                ),
                                criteriaBuilder.like(root.join(Dependency_.library, JoinType.LEFT).get(Library_.groupId), artifactId)
                            )
                        );
                }
            }
            if (criteria.getLicensesShortIdentifier() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLicensesShortIdentifier(),
                            root ->
                                root
                                    .join(Dependency_.library, JoinType.LEFT)
                                    .join(Library_.licenses, JoinType.LEFT)
                                    .get(LicensePerLibrary_.license)
                                    .get(License_.shortIdentifier)
                        )
                    );
            }
            if (criteria.getLibraryRiskId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLibraryRiskId(),
                            root ->
                                root
                                    .join(Dependency_.library, JoinType.LEFT)
                                    .join(Library_.libraryRisk, JoinType.LEFT)
                                    .get(LicenseRisk_.id)
                        )
                    );
            }
            if (criteria.getErrorLogMessage() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getErrorLogMessage(),
                            root ->
                                root
                                    .join(Dependency_.library, JoinType.LEFT)
                                    .join(Library_.errorLogs, JoinType.LEFT)
                                    .get(LibraryErrorLog_.message)
                        )
                    );
            }
            if (criteria.getErrorLogStatus() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getErrorLogStatus(),
                            root ->
                                root
                                    .join(Dependency_.library, JoinType.LEFT)
                                    .join(Library_.errorLogs, JoinType.LEFT)
                                    .get(LibraryErrorLog_.status)
                        )
                    );
            }
            if (criteria.getLibraryCreatedDate() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLibraryCreatedDate(),
                            root -> root.join(Dependency_.library, JoinType.LEFT).get(Library_.createdDate)
                        )
                    );
            }
        }

        return specification;
    }
}
