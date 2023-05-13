package io.github.fossure.repository;

import io.github.fossure.domain.GenericLicenseUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the GenericLicenseUrl entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GenericLicenseUrlRepository extends JpaRepository<GenericLicenseUrl, Long> {}
