package io.github.fossure.web.rest.v1;

import io.github.fossure.domain.LibraryPerProduct;
import io.github.fossure.repository.LibraryPerProductRepository;
import io.github.fossure.service.LibraryPerProductService;
import io.github.fossure.service.criteria.LibraryPerProductCriteria;
import io.github.fossure.service.criteria.query.LibraryPerProductQueryService;
import io.github.fossure.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing {@link LibraryPerProduct}.
 * API base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class LibraryPerProductResource {

    private static final String ENTITY_NAME = "libraryPerProduct";
    private final Logger log = LoggerFactory.getLogger(LibraryPerProductResource.class);
    private final LibraryPerProductService libraryPerProductService;
    private final LibraryPerProductRepository libraryPerProductRepository;
    private final LibraryPerProductQueryService libraryPerProductQueryService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public LibraryPerProductResource(
        LibraryPerProductService libraryPerProductService,
        LibraryPerProductRepository libraryPerProductRepository,
        LibraryPerProductQueryService libraryPerProductQueryService
    ) {
        this.libraryPerProductService = libraryPerProductService;
        this.libraryPerProductRepository = libraryPerProductRepository;
        this.libraryPerProductQueryService = libraryPerProductQueryService;
    }

    /**
     * {@code POST  /library-per-products} : Create a new libraryPerProduct.
     *
     * @param libraryPerProduct the libraryPerProduct to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new libraryPerProduct,
     * or with status {@code 400 (Bad Request)} if the libraryPerProduct has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/library-per-products")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<LibraryPerProduct> createLibraryPerProduct(@RequestBody LibraryPerProduct libraryPerProduct)
        throws URISyntaxException {
        log.debug("REST request to save LibraryPerProduct : {}", libraryPerProduct);
        if (libraryPerProduct.getId() != null) {
            throw new BadRequestAlertException("A new libraryPerProduct cannot already have an ID", ENTITY_NAME, "idexists");
        }
        LibraryPerProduct result = libraryPerProductService.save(libraryPerProduct);
        return ResponseEntity
            .created(new URI("/api/library-per-products/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /library-per-products/:id} : Updates an existing libraryPerProduct.
     *
     * @param id the id of the libraryPerProduct to save.
     * @param libraryPerProduct the libraryPerProduct to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated libraryPerProduct,
     * or with status {@code 400 (Bad Request)} if the libraryPerProduct is not valid,
     * or with status {@code 500 (Internal Server Error)} if the libraryPerProduct couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/library-per-products/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<LibraryPerProduct> updateLibraryPerProduct(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody LibraryPerProduct libraryPerProduct
    ) throws URISyntaxException {
        log.debug("REST request to update LibraryPerProduct : {}, {}", id, libraryPerProduct);
        if (libraryPerProduct.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, libraryPerProduct.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!libraryPerProductRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        LibraryPerProduct result = libraryPerProductService.save(libraryPerProduct);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, libraryPerProduct.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /library-per-products/:id} : Partial updates given fields of an existing libraryPerProduct, field will ignore if it is null
     *
     * @param id the id of the libraryPerProduct to save.
     * @param libraryPerProduct the libraryPerProduct to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated libraryPerProduct,
     * or with status {@code 400 (Bad Request)} if the libraryPerProduct is not valid,
     * or with status {@code 404 (Not Found)} if the libraryPerProduct is not found,
     * or with status {@code 500 (Internal Server Error)} if the libraryPerProduct couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/library-per-products/{id}", consumes = "application/merge-patch+json")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<LibraryPerProduct> partialUpdateLibraryPerProduct(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody LibraryPerProduct libraryPerProduct
    ) throws URISyntaxException {
        log.debug("REST request to partial update LibraryPerProduct partially : {}, {}", id, libraryPerProduct);
        if (libraryPerProduct.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, libraryPerProduct.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!libraryPerProductRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<LibraryPerProduct> result = libraryPerProductService.partialUpdate(libraryPerProduct);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, libraryPerProduct.getId().toString())
        );
    }

    /**
     * {@code GET  /library-per-products/:id} : get the "id" libraryPerProduct.
     *
     * @param id the id of the libraryPerProduct to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the libraryPerProduct,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/library-per-products/{id}")
    public ResponseEntity<LibraryPerProduct> getLibraryPerProduct(@PathVariable Long id) {
        log.debug("REST request to get LibraryPerProduct : {}", id);
        Optional<LibraryPerProduct> libraryPerProduct = libraryPerProductService.findOne(id);
        return ResponseUtil.wrapOrNotFound(libraryPerProduct);
    }

    /**
     * {@code GET  /library-per-products} : get all the libraryPerProducts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of libraryPerProducts in body.
     */
    @GetMapping("/library-per-products")
    public ResponseEntity<List<LibraryPerProduct>> getAllLibraryPerProducts(
        @ParameterObject LibraryPerProductCriteria criteria,
        @ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get LibraryPerProducts by criteria: {}", criteria);
        Page<LibraryPerProduct> page = libraryPerProductQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /library-per-products/count} : count all the libraryPerProducts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/library-per-products/count")
    public ResponseEntity<Long> countLibraryPerProducts(LibraryPerProductCriteria criteria) {
        log.debug("REST request to count LibraryPerProducts by criteria: {}", criteria);
        return ResponseEntity.ok().body(libraryPerProductQueryService.countByCriteria(criteria));
    }

    /**
     * {@code DELETE  /library-per-products/:id} : delete the "id" libraryPerProduct.
     *
     * @param id the id of the libraryPerProduct to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/library-per-products/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> deleteLibraryPerProduct(@PathVariable Long id) {
        log.debug("REST request to delete LibraryPerProduct : {}", id);
        libraryPerProductService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
