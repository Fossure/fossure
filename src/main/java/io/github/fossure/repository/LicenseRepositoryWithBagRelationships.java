package io.github.fossure.repository;

import java.util.List;
import java.util.Optional;
import io.github.fossure.domain.License;
import org.springframework.data.domain.Page;

public interface LicenseRepositoryWithBagRelationships {
    Optional<License> fetchBagRelationships(Optional<License> license);

    List<License> fetchBagRelationships(List<License> licenses);

    Page<License> fetchBagRelationships(Page<License> licenses);
}
