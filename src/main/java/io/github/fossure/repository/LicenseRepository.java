package io.github.fossure.repository;

import io.github.fossure.domain.License;
import io.github.fossure.service.dto.LicenseConflictSimpleDTO;
import io.github.fossure.service.dto.LicenseConflictWithRiskDTO;
import io.github.fossure.service.dto.LicenseSimpleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data SQL repository for the License entity.
 */
@Repository
public interface LicenseRepository extends
    LicenseRepositoryWithBagRelationships, JpaRepository<License, Long>, JpaSpecificationExecutor<License> {
    @Query("select license from License license where license.lastReviewedBy.login = ?#{principal.username}")
    List<License> findByLastReviewedByIsCurrentUser();

    default Optional<License> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<License> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<License> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select distinct license from License license left join fetch license.lastReviewedBy",
        countQuery = "select count(distinct license) from License license"
    )
    Page<License> findAllWithToOneRelationships(Pageable pageable);

    @Query("select distinct license from License license left join fetch license.lastReviewedBy")
    List<License> findAllWithToOneRelationships();

    @Query(
        "select license from License license left join fetch license.lastReviewedBy where license.id =:id"
    )
    Optional<License> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select license from License license where license.shortIdentifier = :shortId")
    Optional<License> findOneByShortIdentifier(@Param("shortId") String shortIdentifier);

    /* @Query("select conflicts from License license left join license.licenseConflicts conflicts where license.id =:id order by conflicts.secondLicenseConflict.shortIdentifier")
    List<LicenseConflict> fetchLicenseConflicts(@Param("id") Long id);*/

    @Query(
        value = "select new io.github.fossure.service.dto.LicenseConflictSimpleDTO(" +
        "conflicts.id, conflicts.secondLicenseConflict.id, conflicts.secondLicenseConflict.fullName, " +
        "conflicts.secondLicenseConflict.shortIdentifier, conflicts.compatibility, conflicts.comment) " +
        "from License license left join license.licenseConflicts conflicts " +
        "where license.id =:id order by conflicts.secondLicenseConflict.shortIdentifier"
    )
    List<LicenseConflictSimpleDTO> fetchLicenseConflicts(@Param("id") Long id);

    @Query(
        value = "select new io.github.fossure.service.dto.LicenseConflictWithRiskDTO(" +
        "conflicts.id, conflicts.secondLicenseConflict.id, conflicts.secondLicenseConflict.fullName, " +
        "conflicts.secondLicenseConflict.shortIdentifier, conflicts.compatibility, conflicts.comment, conflicts.secondLicenseConflict.licenseRisk) " +
        "from License license left join license.licenseConflicts conflicts " +
        "where license.id =:id order by conflicts.secondLicenseConflict.shortIdentifier"
    )
    List<LicenseConflictWithRiskDTO> fetchLicenseConflictsWithRisk(@Param("id") Long id);

    @Query(
        "select new io.github.fossure.service.dto.LicenseSimpleDTO(license.id, license.fullName, license.shortIdentifier) " +
        "from License license order by license.shortIdentifier"
    )
    List<LicenseSimpleDTO> findAllSimpleDTO();

    @Query(
        value = "select distinct license from License license left join fetch license.licenseConflicts",
        countQuery = "select count(distinct license) from License license"
    )
    List<License> findAllWithLicenseConflicts();
}
