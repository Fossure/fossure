package io.github.fossure.web.rest.v1;

import io.github.fossure.domain.LicenseRisk;
import io.github.fossure.repository.LicenseRiskRepository;
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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing {@link LicenseRisk}.
 * API base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class LicenseRiskResource {

    private static final String ENTITY_NAME = "licenseRisk";
    private final Logger log = LoggerFactory.getLogger(LicenseRiskResource.class);
    private final LicenseRiskRepository licenseRiskRepository;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public LicenseRiskResource(LicenseRiskRepository licenseRiskRepository) {
        this.licenseRiskRepository = licenseRiskRepository;
    }

    /**
     * {@code POST  /license-risks} : Create a new licenseRisk.
     *
     * @param licenseRisk the licenseRisk to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new licenseRisk,
     * or with status {@code 400 (Bad Request)} if the licenseRisk has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/license-risks")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<LicenseRisk> createLicenseRisk(@Valid @RequestBody LicenseRisk licenseRisk) throws URISyntaxException {
        log.debug("REST request to save LicenseRisk : {}", licenseRisk);
        if (licenseRisk.getId() != null) {
            throw new BadRequestAlertException("A new licenseRisk cannot already have an ID", ENTITY_NAME, "idexists");
        }
        LicenseRisk result = licenseRiskRepository.save(licenseRisk);
        return ResponseEntity
            .created(new URI("/api/v1/license-risks/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /license-risks/:id} : Updates an existing licenseRisk.
     *
     * @param id          the id of the licenseRisk to save.
     * @param licenseRisk the licenseRisk to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated licenseRisk,
     * or with status {@code 400 (Bad Request)} if the licenseRisk is not valid,
     * or with status {@code 500 (Internal Server Error)} if the licenseRisk couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/license-risks/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<LicenseRisk> updateLicenseRisk(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody LicenseRisk licenseRisk
    ) throws URISyntaxException {
        log.debug("REST request to update LicenseRisk : {}, {}", id, licenseRisk);
        if (licenseRisk.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, licenseRisk.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!licenseRiskRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        LicenseRisk result = licenseRiskRepository.save(licenseRisk);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, licenseRisk.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /license-risks/:id} : Partial updates given fields of an existing licenseRisk, field will ignore if it is null
     *
     * @param id          the id of the licenseRisk to save.
     * @param licenseRisk the licenseRisk to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated licenseRisk,
     * or with status {@code 400 (Bad Request)} if the licenseRisk is not valid,
     * or with status {@code 404 (Not Found)} if the licenseRisk is not found,
     * or with status {@code 500 (Internal Server Error)} if the licenseRisk couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/license-risks/{id}", consumes = "application/merge-patch+json")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<LicenseRisk> partialUpdateLicenseRisk(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody LicenseRisk licenseRisk
    ) throws URISyntaxException {
        log.debug("REST request to partial update LicenseRisk partially : {}, {}", id, licenseRisk);
        if (licenseRisk.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, licenseRisk.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!licenseRiskRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<LicenseRisk> result = licenseRiskRepository
            .findById(licenseRisk.getId())
            .map(existingLicenseRisk -> {
                if (licenseRisk.getName() != null) {
                    existingLicenseRisk.setName(licenseRisk.getName());
                }
                if (licenseRisk.getLevel() != null) {
                    existingLicenseRisk.setLevel(licenseRisk.getLevel());
                }
                if (licenseRisk.getDescription() != null) {
                    existingLicenseRisk.setDescription(licenseRisk.getDescription());
                }
                if (licenseRisk.getColor() != null) {
                    existingLicenseRisk.setColor(licenseRisk.getColor());
                }

                return existingLicenseRisk;
            })
            .map(licenseRiskRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, licenseRisk.getId().toString())
        );
    }

    /**
     * {@code GET  /license-risks} : get all the licenseRisks.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of licenseRisks in body.
     */
    @GetMapping("/license-risks")
    public ResponseEntity<List<LicenseRisk>> getAllLicenseRisks(@ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of LicenseRisks");
        Page<LicenseRisk> page = licenseRiskRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /license-risks/:id} : get the "id" licenseRisk.
     *
     * @param id the id of the licenseRisk to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the licenseRisk,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/license-risks/{id}")
    public ResponseEntity<LicenseRisk> getLicenseRisk(@PathVariable Long id) {
        log.debug("REST request to get LicenseRisk : {}", id);
        Optional<LicenseRisk> licenseRisk = licenseRiskRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(licenseRisk);
    }

    /**
     * {@code DELETE  /license-risks/:id} : delete the "id" licenseRisk.
     *
     * @param id the id of the licenseRisk to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/license-risks/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> deleteLicenseRisk(@PathVariable Long id) {
        log.debug("REST request to delete LicenseRisk : {}", id);
        licenseRiskRepository.deleteById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
