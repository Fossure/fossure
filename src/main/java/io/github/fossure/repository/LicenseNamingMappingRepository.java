package io.github.fossure.repository;

import io.github.fossure.domain.LicenseNamingMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the LicenseNamingMapping entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LicenseNamingMappingRepository extends JpaRepository<LicenseNamingMapping, Long> {}
