package io.github.fossure.repository;

import io.github.fossure.domain.Fossology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the Fossology entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FossologyRepository extends JpaRepository<Fossology, Long> {}
