package io.github.fossure.repository;

import java.util.Optional;
import io.github.fossure.domain.Requirement;
import org.springframework.stereotype.Repository;

/**
 * Custom Spring Data SQL repository for the Requirement entity.
 */
@Repository
public interface RequirementCustomRepository extends RequirementRepository {
    Optional<Requirement> findOneByShortText(String shortText);
}
