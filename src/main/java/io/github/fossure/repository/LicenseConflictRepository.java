package io.github.fossure.repository;

import io.github.fossure.domain.LicenseConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data SQL repository for the LicenseConflict entity.
 */
@Repository
public interface LicenseConflictRepository extends JpaRepository<LicenseConflict, Long> {
    @Query("select licenseConflict from LicenseConflict licenseConflict where licenseConflict.firstLicenseConflict.id = :licenseId")
    List<LicenseConflict> findLicenseConflictsByLicenseId(@Param("licenseId") Long licenseId);

    @Query(
        "select licenseConflict from LicenseConflict licenseConflict where licenseConflict.firstLicenseConflict.id = :licenseId " +
        "and licenseConflict.compatibility = io.github.fossure.domain.enumeration.CompatibilityState.INCOMPATIBLE"
    )
    List<LicenseConflict> findIncompatibleLicenseConflictsByLicenseId(@Param("licenseId") Long licenseId);

    @Query(
        "select licenseConflict from LicenseConflict licenseConflict where licenseConflict.firstLicenseConflict.id = :firstLicenseId " +
        "and licenseConflict.secondLicenseConflict.id = :secondLicenseId " +
        "and licenseConflict.compatibility = io.github.fossure.domain.enumeration.CompatibilityState.INCOMPATIBLE"
    )
    Optional<LicenseConflict> findIncompatibleLicenseConflict(
        @Param("firstLicenseId") Long firstLicenseId,
        @Param("secondLicenseId") Long secondLicenseId
    );

    @Transactional
    @Modifying
    @Query(
        "delete from LicenseConflict licenseConflict where licenseConflict.firstLicenseConflict.id = :licenseId or " +
        "licenseConflict.secondLicenseConflict.id = :licenseId"
    )
    void deleteByLicenseId(@Param("licenseId") Long licenseId);
}
