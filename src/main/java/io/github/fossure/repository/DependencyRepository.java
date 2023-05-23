package io.github.fossure.repository;

import io.github.fossure.domain.Dependency;
import io.github.fossure.domain.Library;
import io.github.fossure.domain.statistics.CountOccurrences;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data SQL repository for the Dependency entity.
 */
@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Long>, JpaSpecificationExecutor<Dependency> {
    @Query(value = "select distinct dependency from Dependency dependency where dependency.project.id = :projectId")
    List<Dependency> findAllByProjectId(@Param("projectId") Long projectId);

    @Query(
        value = "select distinct dependency.library from Dependency dependency where dependency.project.id = :projectId order by dependency.library.name"
    )
    List<Library> findAllLibrariesByProjectId(@Param("projectId") Long projectId);

    @Query(
        value = "select distinct dependency from Dependency dependency where dependency.project.id = :projectId",
        countQuery = "select count(distinct dependency.id) from Dependency dependency where dependency.project.id = :projectId"
    )
    Page<Dependency> findAllByProjectId(Pageable pageable, @Param("projectId") Long projectId);

    @Query("select dependency from Dependency dependency where dependency.project.id = :projectId and dependency.library.id = :libraryId")
    Optional<Dependency> findByProjectIdAndLibraryId(@Param("projectId") Long projectId, @Param("libraryId") Long libraryId);

    @Query(
        "select new io.github.fossure.domain.statistics.CountOccurrences(licenses.license.shortIdentifier, count(*)) " +
        "from Dependency dependency left join dependency.library.licenses licenses where dependency.project.id = :projectId " +
        "group by licenses.license.shortIdentifier order by count(*) desc"
    )
    List<CountOccurrences> countDistributedLicensesByProjectId(@Param("projectId") Long projectId);

    /*
        SELECT license.short_identifier, COUNT(license.short_identifier) FROM LIBRARY_PER_PRODUCT
LEFT JOIN LIBRARY
ON dependency.library_id= library.id
LEFT JOIN LICENSE_PER_LIBRARY
ON library.id = license_per_library.library_id
LEFT JOIN LICENSE
ON license_per_library.license_id = license.id
WHERE dependency.project_id = '19'
GROUP BY license.short_identifier
;
         */

    @Query("select count(distinct dependency) from Dependency dependency where dependency.project.id = :projectId")
    long countLibrariesByProjectId(@Param("projectId") Long projectId);

    @Query("select count(distinct dependency) from Dependency dependency where dependency.project.id = :projectId and dependency.library.reviewed = true")
    long countReviewedLibrariesByProjectId(@Param("projectId") Long projectId);

    @Query("select distinct dependency from Dependency dependency where dependency.project.id = :projectId and dependency.addedManually = true")
    List<Dependency> findDependenciesByProjectIdAndAddedManuallyIsTrue(@Param("projectId") Long projectId);

    @Query(
        "select dependency1.library from Dependency dependency1 where dependency1.project.id = :firstProjectId and not exists " +
        "(select dependency2.library from Dependency dependency2 where dependency2.project.id = :secondProjectId and dependency2.library.id = dependency1.library.id)"
    )
    List<Library> onlyLibrariesFromFirstProjectWithoutIntersection(
        @Param("firstProjectId") Long firstProjectId,
        @Param("secondProjectId") Long secondProjectId
    );

    @Query(
        "select dependency1.library from Dependency dependency1 where dependency1.project.id = :firstProjectId and exists " +
        "(select dependency2.library from Dependency dependency2 where dependency2.project.id = :secondProjectId and dependency2.library.id = dependency1.library.id)"
    )
    List<Library> libraryIntersectionOfProjects(
        @Param("firstProjectId") Long firstProjectId,
        @Param("secondProjectId") Long secondProjectId
    );

    @Transactional
    @Modifying
    @Query("delete from Dependency dependency where dependency.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Transactional
    @Modifying
    @Query("delete from Dependency dependency where dependency.project.id = :projectId and dependency.addedManually = false")
    void deleteByProjectIdAndNotAddedManually(@Param("projectId") Long projectId);
}
