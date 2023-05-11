package io.github.fossure.repository;

import io.github.fossure.domain.LibraryPerProduct;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the LibraryPerProduct entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LibraryPerProductRepository extends JpaRepository<LibraryPerProduct, Long>, JpaSpecificationExecutor<LibraryPerProduct> {}
