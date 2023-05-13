package io.github.fossure.repository;

import io.github.fossure.domain.LicenseRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data SQL repository for the LicenseRisk entity.
 */
@Repository
public interface LicenseRiskRepository extends JpaRepository<LicenseRisk, Long> {
    Optional<LicenseRisk> findOneByName(String name);

    @Override
    @Query("select licenseRisk from LicenseRisk licenseRisk order by licenseRisk.level")
    List<LicenseRisk> findAll();
}
