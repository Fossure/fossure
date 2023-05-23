package io.github.fossure.service;

import io.github.fossure.domain.Dependency;
import io.github.fossure.domain.Library;
import io.github.fossure.domain.statistics.CountOccurrences;
import io.github.fossure.repository.DependencyRepository;
import io.github.fossure.service.criteria.LibraryCriteria;
import io.github.fossure.service.exceptions.LibraryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for managing {@link Dependency}.
 */
@Service
@Transactional
public class DependencyService {

    private final Logger log = LoggerFactory.getLogger(DependencyService.class);

    private final EntityManager entityManager;

    private final DependencyRepository dependencyRepository;

    public DependencyService(DependencyRepository dependencyRepository, EntityManager entityManager) {
        this.dependencyRepository = dependencyRepository;
        this.entityManager = entityManager;
    }

    /**
     * Save a dependency.
     *
     * @param dependency the entity to save.
     * @return the persisted entity.
     */
    public Dependency save(Dependency dependency) {
        log.debug("Request to save Dependency : {}", dependency);
        return dependencyRepository.save(dependency);
    }

    /**
     * Save a dependency. Checks if the library already exist in a project.
     *
     * @param dependency the entity to save.
     * @return the persisted entity.
     */
    public Dependency saveWithCheck(Dependency dependency) throws LibraryException {
        log.debug("Request to save Dependency : {}", dependency);

        if (dependency.getId() == null) {
            Optional<Dependency> optionalDependency = findOneByProjectIdAndLibraryId(
                dependency.getProject().getId(),
                dependency.getLibrary().getId()
            );

            if (optionalDependency.isPresent()) throw new LibraryException(
                "Library [ " +
                dependency.getLibrary().getId() +
                " ] already in Project [ " +
                dependency.getProject().getId() +
                " ]"
            );
        }

        return dependencyRepository.save(dependency);
    }

    /**
     * Partially update a dependency.
     *
     * @param dependency the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Dependency> partialUpdate(Dependency dependency) {
        log.debug("Request to partially update Dependency : {}", dependency);

        return dependencyRepository
            .findById(dependency.getId())
            .map(existingDependency -> {
                if (dependency.getAddedDate() != null) {
                    existingDependency.setAddedDate(dependency.getAddedDate());
                }
                if (dependency.getAddedManually() != null) {
                    existingDependency.setAddedManually(dependency.getAddedManually());
                }
                if (dependency.getHideForPublishing() != null) {
                    existingDependency.setHideForPublishing(dependency.getHideForPublishing());
                }
                if (dependency.getComment() != null) {
                    existingDependency.setComment(dependency.getComment());
                }

                return existingDependency;
            })
            .map(dependencyRepository::save);
    }

    /**
     * Get all the dependencies.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Dependency> findAll(Pageable pageable) {
        log.debug("Request to get all Dependencies");
        return dependencyRepository.findAll(pageable);
    }

    /**
     * Get one dependency by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Dependency> findOne(Long id) {
        log.debug("Request to get Dependency : {}", id);
        return dependencyRepository.findById(id);
    }

    /**
     * Delete the dependency by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Dependency : {}", id);
        dependencyRepository.deleteById(id);
    }

    /**
     * Delete dependencies by ProjectId.
     *
     * @param projectId the id of the project.
     */
    public void deleteByProject(Long projectId) {
        log.debug("Request to delete Dependencies by ProjectId : {}", projectId);
        dependencyRepository.deleteByProjectId(projectId);
    }

    /**
     * Delete dependencies by ProjectId but not libraries that were added manually.
     *
     * @param projectId the id of the project.
     */
    public void deleteByProjectAndNotAddedManually(Long projectId) {
        log.debug("Request to delete Dependencies by ProjectId : {}", projectId);
        dependencyRepository.deleteByProjectIdAndNotAddedManually(projectId);
    }

    /**
     * Get one dependency by ProjectId and LibraryId
     *
     * @param projectId the id of the project entity.
     * @param libraryId the id of the library entity.
     * @return the entity.
     */
    public Optional<Dependency> findOneByProjectIdAndLibraryId(Long projectId, Long libraryId) {
        log.debug("Request to get Dependency by ProjectId : {} and LibraryId : {}", projectId, libraryId);
        return dependencyRepository.findByProjectIdAndLibraryId(projectId, libraryId);
    }

    /**
     * Get all Dependencies from a specific projectId without pagination.
     *
     * @param projectId the ID of a Project.
     * @return the list of Dependencies.
     */
    @Transactional(readOnly = true)
    public List<Dependency> findDependenciesByProjectId(Long projectId) {
        log.debug("Request to get all Libraries by ProjectId : {}", projectId);
        return dependencyRepository
            .findAllByProjectId(projectId)
            .stream()
            .sorted(Comparator.comparing(dependency -> dependency.getLibrary().getName()))
            .collect(Collectors.toList());
    }

    /**
     * Get all Libraries from a specific projectId without pagination.
     *
     * @param projectId the ID of a Project.
     * @return the list of Libraries.
     */
    @Transactional(readOnly = true)
    public List<Library> findLibrariesByProjectId(Long projectId) {
        log.debug("Request to get all Libraries by ProjectId : {}", projectId);
        return dependencyRepository.findAllLibrariesByProjectId(projectId);
    }

    /**
     * Get all Libraries from a specific projectId with pagination.
     *
     * @param pageable  the pagination information.
     * @param projectId the ID of a Project.
     * @return the list of Libraries.
     */
    @Transactional(readOnly = true)
    public Page<Library> findLibrariesByProjectId(LibraryCriteria libraryCriteria, Pageable pageable, Long projectId) {
        log.debug("Request to get all Libraries by ProjectId : {}", projectId);

        /* Default query to get pageable result with filter and many-to-many relationship is not possible.
         * Solution is to create two queries.
         * 1. First query to get a list with all library IDs filtered by a projectId.
         * 2. Second query to get a list based on the first result the concrete libraries with relationships.
         *
         * Exception: firstResult/maxResults specified with collection fetch.
         * In memory pagination was about to be applied.
         * Failing because 'Fail on pagination over collection fetch' is enabled.
         */

        /* TEST -> */
        /* Specification<Library> specification = libraryQueryService.createSpecification(libraryCriteria);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Library> query = builder.createQuery(Library.class);

        Root<Library> root = query.from(Library.class);
        Predicate predicate = specification.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        query.select(root);*/

        /* <- TEST END */

        long count = (long) entityManager
            .createQuery("select count(lpp.id) from Dependency lpp where lpp.project.id = :projectId")
            .setParameter("projectId", projectId)
            .getSingleResult();

        List<Long> libraryIds = entityManager
            .createQuery(
                "select distinct lpp.library from Dependency lpp where lpp.project.id = :projectId order by lpp.library.name",
                Library.class
            )
            .setParameter("projectId", projectId)
            .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
            .setMaxResults(pageable.getPageSize())
            .getResultStream()
            .map(Library::getId)
            .collect(Collectors.toList());

        List<Library> libraries = entityManager
            .createQuery(
                "select distinct library from Library library left join fetch library.licenseToPublishes where library.id in (:libraryIds) order by library.name",
                Library.class
            )
            .setParameter("libraryIds", libraryIds)
            .getResultList();

        return new PageImpl<>(libraries, pageable, count);
        //return dependencyRepository.findAllLibrariesByProjectId(pageable, projectId);
    }

    public List<CountOccurrences> countDistributedLicensesByProjectId(Long projectId) {
        return dependencyRepository.countDistributedLicensesByProjectId(projectId);
    }

    public long countLibrariesByProject(Long projectId) {
        return dependencyRepository.countLibrariesByProjectId(projectId);
    }

    public long countReviewedLibrariesByProject(Long projectId) {
        return dependencyRepository.countReviewedLibrariesByProjectId(projectId);
    }

    public List<Dependency> findDependenciesByProjectIdAndAddedManuallyIsTrue(Long projectId) {
        return dependencyRepository.findDependenciesByProjectIdAndAddedManuallyIsTrue(projectId);
    }

    public List<Library> onlyLibrariesFromFirstProjectWithoutIntersection(Long firstProjectId, Long secondProjectId) {
        return dependencyRepository.onlyLibrariesFromFirstProjectWithoutIntersection(firstProjectId, secondProjectId);
    }

    public List<Library> libraryIntersectionOfProjects(Long firstProjectId, Long secondProjectId) {
        return dependencyRepository.libraryIntersectionOfProjects(firstProjectId, secondProjectId);
    }
}
