package io.github.fossure.repository;

import io.github.fossure.domain.LicenseConflict;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the LicenseConflict entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LicenseConflictRepository extends JpaRepository<LicenseConflict, Long> {}
