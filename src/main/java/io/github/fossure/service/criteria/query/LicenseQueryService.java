package io.github.fossure.service.criteria.query;

import io.github.fossure.domain.*;
import io.github.fossure.repository.LicenseRepository;
import io.github.fossure.service.criteria.LicenseCriteria;
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
 * Service for executing complex queries for {@link License} entities in the database.
 * The main input is a {@link LicenseCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link License} or a {@link Page} of {@link License} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class LicenseQueryService extends QueryService<License> {

    private final Logger log = LoggerFactory.getLogger(LicenseQueryService.class);

    private final LicenseRepository licenseRepository;

    public LicenseQueryService(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    /**
     * Return a {@link List} of {@link License} which matches the criteria from the database.
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<License> findByCriteria(LicenseCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<License> specification = createSpecification(criteria);
        return licenseRepository.findAll(specification);
    }

    /**
     * Return a {@link Page} of {@link License} which matches the criteria from the database.
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page     The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<License> findByCriteria(LicenseCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<License> specification = createSpecification(criteria);
        return licenseRepository.findAll(specification, page);
    }

    /**
     * Return the number of matching entities in the database.
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(LicenseCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<License> specification = createSpecification(criteria);
        return licenseRepository.count(specification);
    }

    /**
     * Function to convert {@link LicenseCriteria} to a {@link Specification}
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<License> createSpecification(LicenseCriteria criteria) {
        Specification<License> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), License_.id));
            }
            if (criteria.getFullName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getFullName(), License_.fullName));
            }
            if (criteria.getShortIdentifier() != null) {
                specification = specification.and(buildStringSpecification(criteria.getShortIdentifier(), License_.shortIdentifier));
            }
            if (criteria.getSpdxIdentifier() != null) {
                specification = specification.and(buildStringSpecification(criteria.getSpdxIdentifier(), License_.spdxIdentifier));
            }
            if (criteria.getUrl() != null) {
                specification = specification.and(buildStringSpecification(criteria.getUrl(), License_.url));
            }
            if (criteria.getOther() != null) {
                specification = specification.and(buildStringSpecification(criteria.getOther(), License_.other));
            }
            if (criteria.getReviewed() != null) {
                specification = specification.and(buildSpecification(criteria.getReviewed(), License_.reviewed));
            }
            if (criteria.getLastReviewedDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getLastReviewedDate(), License_.lastReviewedDate));
            }
            if (criteria.getLastReviewedByLogin() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLastReviewedByLogin(),
                            root -> root.join(License_.lastReviewedBy, JoinType.LEFT).get(User_.login)
                        )
                    );
            }
            if (criteria.getLicenseRiskId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLicenseRiskId(),
                            root -> root.join(License_.licenseRisk, JoinType.LEFT).get(LicenseRisk_.id)
                        )
                    );
            }
            if (criteria.getRequirementShortText() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getRequirementShortText(),
                            root -> root.join(License_.requirements, JoinType.LEFT).get(Requirement_.shortText)
                        )
                    );
            }
            /*if (criteria.getLibraryId() != null) {
                specification =
                    specification.and(
                        buildSpecification(criteria.getLibraryId(), root -> root.join(License_.libraries, JoinType.LEFT).get(Library_.id))
                    );
            }*/
            if (criteria.getLibraryPublishId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLibraryPublishId(),
                            root -> root.join(License_.libraryPublishes, JoinType.LEFT).get(Library_.id)
                        )
                    );
            }
            if (criteria.getLibraryFilesId() != null) {
                specification =
                    specification.and(
                        buildSpecification(
                            criteria.getLibraryFilesId(),
                            root -> root.join(License_.libraryFiles, JoinType.LEFT).get(Library_.id)
                        )
                    );
            }
            if (criteria.getName() != null && criteria.getName().getEquals() != null) {
                specification =
                    specification.and((root, query, cb) ->
                        cb.or(
                            cb.equal(cb.upper(root.get(License_.fullName)), criteria.getName().getEquals().toUpperCase()),
                            cb.equal(cb.upper(root.get(License_.shortIdentifier)), criteria.getName().getEquals().toUpperCase())
                        )
                    );
            }
        }
        return specification;
    }
}
