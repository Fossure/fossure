package io.github.fossure.web.rest.v1;

import io.github.fossure.domain.Project;
import io.github.fossure.domain.Upload;
import io.github.fossure.domain.enumeration.EntityUploadChoice;
import io.github.fossure.domain.enumeration.UploadState;
import io.github.fossure.repository.UploadRepository;
import io.github.fossure.service.ProjectService;
import io.github.fossure.service.UploadService;
import io.github.fossure.service.exceptions.UploadException;
import io.github.fossure.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
 * REST controller for managing {@link Upload}.
 * API base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class UploadResource {

    private static final Logger log = LoggerFactory.getLogger(UploadResource.class);

    private static final String ENTITY_NAME = "upload";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UploadService uploadService;
    private final UploadRepository uploadRepository;
    private final ProjectService projectService;

    public UploadResource(
        UploadService uploadService,
        UploadRepository uploadRepository,
        ProjectService projectService
    ) {
        this.uploadService = uploadService;
        this.uploadRepository = uploadRepository;
        this.projectService = projectService;
    }

    /**
     * {@code POST  /uploads} : Create a new upload.
     *
     * @param upload the upload to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new upload,
     * or with status {@code 400 (Bad Request)} if the upload has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/uploads")
    public ResponseEntity<Upload> createUpload(@Valid @RequestBody Upload upload) throws URISyntaxException {
        log.debug("REST request to save Upload : {}", upload);
        if (upload.getId() != null) {
            throw new BadRequestAlertException("A new upload cannot already have an ID", ENTITY_NAME, "idexists");
        }

        try {
            if (upload.getEntityToUpload() == EntityUploadChoice.PROJECT) {
                Optional<Project> optionalProject = projectService.findOne(Long.valueOf(upload.getRecord()));

                if (optionalProject.isPresent()) {
                    Project project = optionalProject.get();
                    project.setUploadState(UploadState.PROCESSING);
                    projectService.save(project);
                }
            }
            uploadService.uploadHandler(upload);
        } catch (UploadException e) {
            if (upload.getEntityToUpload() == EntityUploadChoice.PROJECT) {
                Optional<Project> optionalProject = projectService.findOne(Long.valueOf(upload.getRecord()));

                if (optionalProject.isPresent()) {
                    Project project = optionalProject.get();
                    project.setUploadState(UploadState.FAILURE);
                    projectService.save(project);
                }
            }
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "uploaderror");
        }

        Upload result = uploadService.save(upload);
        return ResponseEntity
            .created(new URI("/api/v1/uploads/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /uploads/:id} : Updates an existing upload.
     *
     * @param id the id of the upload to save.
     * @param upload the upload to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated upload,
     * or with status {@code 400 (Bad Request)} if the upload is not valid,
     * or with status {@code 500 (Internal Server Error)} if the upload couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/uploads/{id}")
    public ResponseEntity<Upload> updateUpload(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Upload upload
    ) throws URISyntaxException {
        log.debug("REST request to update Upload : {}, {}", id, upload);
        if (upload.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, upload.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!uploadRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Upload result = uploadService.save(upload);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, upload.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /uploads/:id} : Partial updates given fields of an existing upload, field will ignore if it is null
     *
     * @param id the id of the upload to save.
     * @param upload the upload to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated upload,
     * or with status {@code 400 (Bad Request)} if the upload is not valid,
     * or with status {@code 404 (Not Found)} if the upload is not found,
     * or with status {@code 500 (Internal Server Error)} if the upload couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/uploads/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Upload> partialUpdateUpload(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Upload upload
    ) throws URISyntaxException {
        log.debug("REST request to partial update Upload partially : {}, {}", id, upload);
        if (upload.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, upload.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!uploadRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Upload> result = uploadService.partialUpdate(upload);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, upload.getId().toString())
        );
    }

    /**
     * {@code GET  /uploads} : get all the uploads.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of uploads in body.
     */
    @GetMapping("/uploads")
    public ResponseEntity<List<Upload>> getAllUploads(@ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Uploads");
        Page<Upload> page = uploadService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /uploads/:id} : get the "id" upload.
     *
     * @param id the id of the upload to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the upload,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/uploads/{id}")
    public ResponseEntity<Upload> getUpload(@PathVariable Long id) {
        log.debug("REST request to get Upload : {}", id);
        Optional<Upload> upload = uploadService.findOne(id);
        return ResponseUtil.wrapOrNotFound(upload);
    }

    /**
     * {@code DELETE  /uploads/:id} : delete the "id" upload.
     *
     * @param id the id of the upload to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/uploads/{id}")
    public ResponseEntity<Void> deleteUpload(@PathVariable Long id) {
        log.debug("REST request to delete Upload : {}", id);
        uploadService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
