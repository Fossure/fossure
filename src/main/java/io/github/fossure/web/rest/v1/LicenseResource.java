package io.github.fossure.web.rest.v1;

import io.github.fossure.domain.File;
import io.github.fossure.domain.License;
import io.github.fossure.domain.enumeration.ExportFormat;
import io.github.fossure.repository.LicenseRepository;
import io.github.fossure.service.LicenseService;
import io.github.fossure.service.criteria.LicenseCriteria;
import io.github.fossure.service.criteria.query.LicenseQueryService;
import io.github.fossure.service.dto.LicenseConflictSimpleDTO;
import io.github.fossure.service.dto.LicenseConflictWithRiskDTO;
import io.github.fossure.service.dto.LicenseSimpleDTO;
import io.github.fossure.service.exceptions.ExportException;
import io.github.fossure.service.exceptions.LicenseException;
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
 * REST controller for managing {@link License}.
 * API base path: {@code /api/v1}
 */
@RestController("LicenseCustomResource")
@RequestMapping("/api/v1")
public class LicenseResource {

    private static final Logger log = LoggerFactory.getLogger(LicenseResource.class);

    private static final String ENTITY_NAME = "license";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LicenseService licenseService;
    private final LicenseRepository licenseRepository;
    private final LicenseQueryService licenseQueryService;

    public LicenseResource(
        LicenseService licenseService,
        LicenseRepository licenseRepository,
        LicenseQueryService licenseQueryService
    ) {
        this.licenseService = licenseService;
        this.licenseRepository = licenseRepository;
        this.licenseQueryService = licenseQueryService;
    }

    /**
     * {@code POST  /licenses} : Create a new license.
     *
     * @param license the license to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new license,
     * or with status {@code 400 (Bad Request)} if the license has already an ID or the license check fails.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/licenses")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<License> createLicense(@Valid @RequestBody License license) throws URISyntaxException {
        log.debug("REST request to save License : {}", license);
        if (license.getId() != null) {
            throw new BadRequestAlertException("A new license cannot already have an ID", ENTITY_NAME, "idexists");
        }
        try {
            License result = licenseService.saveWithCheck(license);
            licenseService.createLicenseConflictsForNewLicense(result);
            licenseService.createLicenseConflictsForExistingLicenses(result);

            return ResponseEntity
                .created(new URI("/api/v1/licenses/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                .body(result);
        } catch (LicenseException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "licenseerror");
        }
    }

    /**
     * {@code PUT  /licenses/:id} : Updates an existing license.
     *
     * @param id      the id of the license to save.
     * @param license the license to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated license,
     * or with status {@code 400 (Bad Request)} if the license is not valid or the license check fails,
     * or with status {@code 500 (Internal Server Error)} if the license couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/licenses/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<License> updateLicense(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody License license
    ) throws URISyntaxException {
        log.debug("REST request to update License : {}, {}", id, license);
        if (license.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, license.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!licenseRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        try {
            License result = licenseService.saveWithCheck(license);

            return ResponseEntity
                .ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, license.getId().toString()))
                .body(result);
        } catch (LicenseException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "licenseerror");
        }
    }

    /**
     * {@code PATCH  /licenses/:id} : Partial updates given fields of an existing license, field will ignore if it is null
     *
     * @param id      the id of the license to save.
     * @param license the license to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated license,
     * or with status {@code 400 (Bad Request)} if the license is not valid,
     * or with status {@code 404 (Not Found)} if the license is not found,
     * or with status {@code 500 (Internal Server Error)} if the license couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/licenses/{id}", consumes = "application/merge-patch+json")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<License> partialUpdateLicense(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody License license
    ) throws URISyntaxException {
        log.debug("REST request to partial update License partially : {}, {}", id, license);
        if (license.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, license.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!licenseRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<License> result = licenseService.partialUpdate(license);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, license.getId().toString())
        );
    }

    /**
     * {@code GET  /licenses/count} : count all the licenses.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/licenses/count")
    public ResponseEntity<Long> countLicenses(LicenseCriteria criteria) {
        log.debug("REST request to count Licenses by criteria: {}", criteria);
        return ResponseEntity.ok().body(licenseQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /licenses/:id} : get the "id" license.
     *
     * @param id the id of the license to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the license, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/licenses/{id}")
    public ResponseEntity<License> getLicense(@PathVariable Long id) {
        log.debug("REST request to get License : {}", id);
        Optional<License> license = licenseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(license);
    }

    /**
     * {@code GET  /licenses} : get all the licenses.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of licenses in body.
     */
    @GetMapping("/licenses")
    public ResponseEntity<List<License>> getAllLicenses(@ParameterObject LicenseCriteria criteria, @ParameterObject Pageable pageable) {
        log.debug("REST request to get Licenses by criteria: {}", criteria);
        Page<License> page = licenseQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code DELETE  /licenses/:id} : delete the "id" license.
     *
     * @param id the id of the license to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/licenses/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> deleteLicense(@PathVariable Long id) {
        log.debug("REST request to delete License : {}", id);
        licenseService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET /licenses/export} : Export all license entries.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body a {@link File} with licenses,
     * or with status {@code 400 (Bad Request)} if licenses could not be serialized.
     */
    @GetMapping("/licenses/export")
    public ResponseEntity<File> exportLicenses(@RequestParam(value = "format", required = false) String format) {
        log.debug("REST request to export license table as : {}", format);
        ExportFormat exportFormat = ExportFormat.getExportFormatByValue(format.toLowerCase());

        if (exportFormat == null) throw new BadRequestAlertException("Unsupported export format", ENTITY_NAME, "exporterror");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"licenses.json\"; filename*=\"licenses.json\"");
            return ResponseEntity.ok().headers(headers).body(licenseService.export(exportFormat));
        } catch (ExportException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "exporterror");
        }
    }

    /**
     * {@code GET /licenses/:id/license-conflicts} :
     *
     * @param id License ID
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body a list of
     * {@link LicenseConflictSimpleDTO LicenseConflictSimpleDTOs}.
     */
    @GetMapping("/licenses/{id}/license-conflicts")
    public ResponseEntity<List<LicenseConflictSimpleDTO>> fetchLicenseConflicts(@PathVariable Long id) {
        log.debug("REST request to get license conflicts by ID : {}", id);

        return ResponseEntity.ok().body(licenseService.fetchLicenseConflicts(id));
    }

    /**
     * {@code GET /licenses/:id/license-conflicts-with-risk} :
     *
     * @param id License ID
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body a list of
     * {@link LicenseConflictWithRiskDTO LicenseConflictWithRiskDTOs}.
     */
    @GetMapping("/licenses/{id}/license-conflicts-with-risk")
    public ResponseEntity<List<LicenseConflictWithRiskDTO>> fetchLicenseConflictsWithRisk(@PathVariable Long id) {
        log.debug("REST request to get license conflicts by ID : {}", id);

        return ResponseEntity.ok().body(licenseService.fetchLicenseConflictsWithRisk(id));
    }

    /**
     * {@code GET /licenses/license-names} :
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body a list of
     * {@link LicenseSimpleDTO LicenseSimpleDTOs}.
     */
    @GetMapping("/licenses/license-names")
    public ResponseEntity<List<LicenseSimpleDTO>> getAllLicenseNames() {
        log.debug("REST request to get all license names");

        return ResponseEntity.ok().body(licenseService.findAllSimpleDTO());
    }
}
