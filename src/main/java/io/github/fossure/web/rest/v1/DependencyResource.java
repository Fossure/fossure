package io.github.fossure.web.rest.v1;

import io.github.fossure.domain.Dependency;
import io.github.fossure.repository.DependencyRepository;
import io.github.fossure.service.DependencyService;
import io.github.fossure.service.criteria.DependencyCriteria;
import io.github.fossure.service.criteria.query.DependencyQueryService;
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
 * REST controller for managing {@link Dependency}.
 * API base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class DependencyResource {

    private static final String ENTITY_NAME = "dependency";
    private final Logger log = LoggerFactory.getLogger(DependencyResource.class);
    private final DependencyService dependencyService;
    private final DependencyRepository dependencyRepository;
    private final DependencyQueryService dependencyQueryService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public DependencyResource(
        DependencyService dependencyService,
        DependencyRepository dependencyRepository,
        DependencyQueryService dependencyQueryService
    ) {
        this.dependencyService = dependencyService;
        this.dependencyRepository = dependencyRepository;
        this.dependencyQueryService = dependencyQueryService;
    }

    /**
     * {@code POST  /dependencies} : Create a new dependency.
     *
     * @param dependency the dependency to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new dependency,
     * or with status {@code 400 (Bad Request)} if the dependency has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/dependencies")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Dependency> createDependency(@RequestBody Dependency dependency)
        throws URISyntaxException {
        log.debug("REST request to save Dependency : {}", dependency);
        if (dependency.getId() != null) {
            throw new BadRequestAlertException("A new dependency cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Dependency result = dependencyService.save(dependency);
        return ResponseEntity
            .created(new URI("/api/v1/dependencies/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /dependencies/:id} : Updates an existing dependency.
     *
     * @param id the id of the dependency to save.
     * @param dependency the dependency to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated dependency,
     * or with status {@code 400 (Bad Request)} if the dependency is not valid,
     * or with status {@code 500 (Internal Server Error)} if the dependency couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/dependencies/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Dependency> updateDependency(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Dependency dependency
    ) throws URISyntaxException {
        log.debug("REST request to update Dependency : {}, {}", id, dependency);
        if (dependency.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dependency.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!dependencyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Dependency result = dependencyService.save(dependency);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, dependency.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /dependencies/:id} : Partial updates given fields of an existing dependency, field will ignore if it is null
     *
     * @param id the id of the dependency to save.
     * @param dependency the dependency to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated dependency,
     * or with status {@code 400 (Bad Request)} if the dependency is not valid,
     * or with status {@code 404 (Not Found)} if the dependency is not found,
     * or with status {@code 500 (Internal Server Error)} if the dependency couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/dependencies/{id}", consumes = "application/merge-patch+json")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Dependency> partialUpdateDependency(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Dependency dependency
    ) throws URISyntaxException {
        log.debug("REST request to partial update Dependency partially : {}, {}", id, dependency);
        if (dependency.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dependency.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!dependencyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Dependency> result = dependencyService.partialUpdate(dependency);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, dependency.getId().toString())
        );
    }

    /**
     * {@code GET  /dependencies/:id} : get the "id" dependency.
     *
     * @param id the id of the dependency to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the dependency,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/dependencies/{id}")
    public ResponseEntity<Dependency> getDependency(@PathVariable Long id) {
        log.debug("REST request to get Dependency : {}", id);
        Optional<Dependency> dependency = dependencyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(dependency);
    }

    /**
     * {@code GET  /dependencies} : get all the dependencies.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of dependencies in body.
     */
    @GetMapping("/dependencies")
    public ResponseEntity<List<Dependency>> getAllDependencies(
        @ParameterObject DependencyCriteria criteria,
        @ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get Dependencies by criteria: {}", criteria);
        Page<Dependency> page = dependencyQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /dependencies/count} : count all the dependencies.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/dependencies/count")
    public ResponseEntity<Long> countDependencies(DependencyCriteria criteria) {
        log.debug("REST request to count Dependencies by criteria: {}", criteria);
        return ResponseEntity.ok().body(dependencyQueryService.countByCriteria(criteria));
    }

    /**
     * {@code DELETE  /dependencies/:id} : delete the "id" dependency.
     *
     * @param id the id of the dependency to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/dependencies/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> deleteDependency(@PathVariable Long id) {
        log.debug("REST request to delete Dependency : {}", id);
        dependencyService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
