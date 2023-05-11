package io.github.fossure.repository;

import java.util.List;
import java.util.Optional;
import io.github.fossure.domain.Library;
import org.springframework.data.domain.Page;

public interface LibraryRepositoryWithBagRelationships {
    Optional<Library> fetchBagRelationships(Optional<Library> library);

    List<Library> fetchBagRelationships(List<Library> libraries);

    Page<Library> fetchBagRelationships(Page<Library> libraries);
}
