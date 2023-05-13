package io.github.fossure.service;

import io.github.fossure.domain.Library;
import io.github.fossure.domain.LibraryPerProduct;
import io.github.fossure.domain.statistics.CountOccurrences;
import io.github.fossure.repository.LibraryPerProductRepository;
import io.github.fossure.service.criteria.LibraryCriteria;
import io.github.fossure.service.exceptions.LibraryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for managing {@link LibraryPerProduct}.
 */
@Service
@Transactional
public class LibraryPerProductService {

    private final Logger log = LoggerFactory.getLogger(LibraryPerProductService.class);

    private final EntityManager entityManager;

    private final LibraryPerProductRepository libraryPerProductRepository;

    public LibraryPerProductService(LibraryPerProductRepository libraryPerProductRepository, EntityManager entityManager) {
        this.libraryPerProductRepository = libraryPerProductRepository;
        this.entityManager = entityManager;
    }

    /**
     * Save a libraryPerProduct.
     *
     * @param libraryPerProduct the entity to save.
     * @return the persisted entity.
     */
    public LibraryPerProduct save(LibraryPerProduct libraryPerProduct) {
        log.debug("Request to save LibraryPerProduct : {}", libraryPerProduct);
        return libraryPerProductRepository.save(libraryPerProduct);
    }

    /**
     * Save a libraryPerProduct. Checks if the library already exist in a product.
     *
     * @param libraryPerProduct the entity to save.
     * @return the persisted entity.
     */
    public LibraryPerProduct saveWithCheck(LibraryPerProduct libraryPerProduct) throws LibraryException {
        log.debug("Request to save LibraryPerProduct : {}", libraryPerProduct);

        if (libraryPerProduct.getId() == null) {
            Optional<LibraryPerProduct> optionalLibraryPerProduct = findOneByProductIdAndLibraryId(
                libraryPerProduct.getProduct().getId(),
                libraryPerProduct.getLibrary().getId()
            );

            if (optionalLibraryPerProduct.isPresent()) throw new LibraryException(
                "Library [ " +
                libraryPerProduct.getLibrary().getId() +
                " ] already in Product [ " +
                libraryPerProduct.getProduct().getId() +
                " ]"
            );
        }

        return libraryPerProductRepository.save(libraryPerProduct);
    }

    /**
     * Partially update a libraryPerProduct.
     *
     * @param libraryPerProduct the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<LibraryPerProduct> partialUpdate(LibraryPerProduct libraryPerProduct) {
        log.debug("Request to partially update LibraryPerProduct : {}", libraryPerProduct);

        return libraryPerProductRepository
            .findById(libraryPerProduct.getId())
            .map(existingLibraryPerProduct -> {
                if (libraryPerProduct.getAddedDate() != null) {
                    existingLibraryPerProduct.setAddedDate(libraryPerProduct.getAddedDate());
                }
                if (libraryPerProduct.getAddedManually() != null) {
                    existingLibraryPerProduct.setAddedManually(libraryPerProduct.getAddedManually());
                }
                if (libraryPerProduct.getHideForPublishing() != null) {
                    existingLibraryPerProduct.setHideForPublishing(libraryPerProduct.getHideForPublishing());
                }
                if (libraryPerProduct.getComment() != null) {
                    existingLibraryPerProduct.setComment(libraryPerProduct.getComment());
                }

                return existingLibraryPerProduct;
            })
            .map(libraryPerProductRepository::save);
    }

    /**
     * Get all the libraryPerProducts.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<LibraryPerProduct> findAll(Pageable pageable) {
        log.debug("Request to get all LibraryPerProducts");
        return libraryPerProductRepository.findAll(pageable);
    }

    /**
     * Get one libraryPerProduct by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<LibraryPerProduct> findOne(Long id) {
        log.debug("Request to get LibraryPerProduct : {}", id);
        return libraryPerProductRepository.findById(id);
    }

    /**
     * Delete the libraryPerProduct by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete LibraryPerProduct : {}", id);
        libraryPerProductRepository.deleteById(id);
    }

    /**
     * Delete libraryPerProducts by ProductId.
     *
     * @param productId the id of the product.
     */
    public void deleteByProduct(Long productId) {
        log.debug("Request to delete LibraryPerProducts by ProductId : {}", productId);
        libraryPerProductRepository.deleteByProductId(productId);
    }

    /**
     * Delete libraryPerProducts by ProductId but not libraries that were added manually.
     *
     * @param productId the id of the product.
     */
    public void deleteByProductAndNotAddedManually(Long productId) {
        log.debug("Request to delete LibraryPerProducts by ProductId : {}", productId);
        libraryPerProductRepository.deleteByProductIdAndNotAddedManually(productId);
    }

    /**
     * Get one libraryPerProduct by ProductId and LibraryId
     *
     * @param productId the id of the product entity.
     * @param libraryId the id of the library entity.
     * @return the entity.
     */
    public Optional<LibraryPerProduct> findOneByProductIdAndLibraryId(Long productId, Long libraryId) {
        log.debug("Request to get LibraryPerProduct by ProductId : {} and LibraryId : {}", productId, libraryId);
        return libraryPerProductRepository.findByProductIdAndLibraryId(productId, libraryId);
    }

    /**
     * Get all LibraryPerProducts from a specific productId without pagination.
     *
     * @param productId the ID of a Product.
     * @return the list of LibraryPerProducts.
     */
    @Transactional(readOnly = true)
    public List<LibraryPerProduct> findLibraryPerProductsByProductId(Long productId) {
        log.debug("Request to get all Libraries by ProductId : {}", productId);
        return libraryPerProductRepository
            .findAllByProductId(productId)
            .stream()
            .sorted(Comparator.comparing(lpp -> lpp.getLibrary().getArtifactId()))
            .collect(Collectors.toList());
    }

    /**
     * Get all Libraries from a specific productId without pagination.
     *
     * @param productId the ID of a Product.
     * @return the list of Libraries.
     */
    @Transactional(readOnly = true)
    public List<Library> findLibrariesByProductId(Long productId) {
        log.debug("Request to get all Libraries by ProductId : {}", productId);
        return libraryPerProductRepository.findAllLibrariesByProductId(productId);
    }

    /**
     * Get all Libraries from a specific productId with pagination.
     *
     * @param pageable  the pagination information.
     * @param productId the ID of a Product.
     * @return the list of Libraries.
     */
    @Transactional(readOnly = true)
    public Page<Library> findLibrariesByProductId(LibraryCriteria libraryCriteria, Pageable pageable, Long productId) {
        log.debug("Request to get all Libraries by ProductId : {}", productId);

        /* Default query to get pageable result with filter and many-to-many relationship is not possible.
         * Solution is to create two queries.
         * 1. First query to get a list with all library IDs filtered by a productId.
         * 2. Second query to get a list based on the first result the concrete libraries with relationships.
         *
         * Exception: firstResult/maxResults specified with collection fetch.
         * In memory pagination was about to be applied.
         * Failing because 'Fail on pagination over collection fetch' is enabled.
         */

        /* TEST -> */
        /* Specification<Library> specification = libraryQueryService.createSpecification(libraryCriteria);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Library> query = builder.createQuery(Library.class);

        Root<Library> root = query.from(Library.class);
        Predicate predicate = specification.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        query.select(root);*/

        /* <- TEST END */

        long count = (long) entityManager
            .createQuery("select count(lpp.id) from LibraryPerProduct lpp where lpp.product.id = :productId")
            .setParameter("productId", productId)
            .getSingleResult();

        List<Long> libraryIds = entityManager
            .createQuery(
                "select distinct lpp.library from LibraryPerProduct lpp where lpp.product.id = :productId order by lpp.library.artifactId",
                Library.class
            )
            .setParameter("productId", productId)
            .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
            .setMaxResults(pageable.getPageSize())
            .getResultStream()
            .map(Library::getId)
            .collect(Collectors.toList());

        List<Library> libraries = entityManager
            .createQuery(
                "select distinct library from Library library left join fetch library.licenseToPublishes where library.id in (:libraryIds) order by library.artifactId",
                Library.class
            )
            .setParameter("libraryIds", libraryIds)
            .getResultList();

        return new PageImpl<>(libraries, pageable, count);
        //return libraryPerProductRepository.findAllLibrariesByProductId(pageable, productId);
    }

    public List<CountOccurrences> countDistributedLicensesByProductId(Long productId) {
        return libraryPerProductRepository.countDistributedLicensesByProductId(productId);
    }

    public long countLibrariesByProduct(Long productId) {
        return libraryPerProductRepository.countLibrariesByProductId(productId);
    }

    public long countReviewedLibrariesByProduct(Long productId) {
        return libraryPerProductRepository.countReviewedLibrariesByProductId(productId);
    }

    public List<LibraryPerProduct> findLibraryPerProductsByProductIdAndAddedManuallyIsTrue(Long productId) {
        return libraryPerProductRepository.findLibraryPerProductsByProductIdAndAddedManuallyIsTrue(productId);
    }

    public List<Library> onlyLibrariesFromFirstProductWithoutIntersection(Long firstProductId, Long secondProductId) {
        return libraryPerProductRepository.onlyLibrariesFromFirstProductWithoutIntersection(firstProductId, secondProductId);
    }

    public List<Library> libraryIntersectionOfProducts(Long firstProductId, Long secondProductId) {
        return libraryPerProductRepository.libraryIntersectionOfProducts(firstProductId, secondProductId);
    }
}
