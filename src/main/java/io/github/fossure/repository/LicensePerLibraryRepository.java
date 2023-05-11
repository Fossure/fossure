package io.github.fossure.repository;

import io.github.fossure.domain.LicensePerLibrary;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the LicensePerLibrary entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LicensePerLibraryRepository extends JpaRepository<LicensePerLibrary, Long>, JpaSpecificationExecutor<LicensePerLibrary> {}
