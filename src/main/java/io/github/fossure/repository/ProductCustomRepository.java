package io.github.fossure.repository;

import java.util.Optional;
import io.github.fossure.domain.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Custom Spring Data SQL repository for the Product entity.
 */
@Repository
public interface ProductCustomRepository extends ProductRepository {
    @Query(
        "select product from Product product where lower(product.identifier) = lower(:identifier) and lower(product.version) = lower(:version)"
    )
    Optional<Product> findByIdentifierAndVersion(@Param("identifier") String identifier, @Param("version") String version);

    @Query("select product from Product product where product.identifier = :identifier and product.delivered = false")
    Optional<Product> findByIdAndDeliveredIsFalse(@Param("identifier") String identifier);

    @Query("select count(product) from Product product where product.delivered = false")
    long countAllByDeliveredIsFalse();
}
