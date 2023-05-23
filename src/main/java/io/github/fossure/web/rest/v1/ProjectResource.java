package io.github.fossure.web.rest.v1;

import io.github.fossure.config.ApplicationProperties;
import io.github.fossure.config.Constants;
import io.github.fossure.domain.File;
import io.github.fossure.domain.Library;
import io.github.fossure.domain.Project;
import io.github.fossure.domain.enumeration.*;
import io.github.fossure.domain.helper.BasicAuthentication;
import io.github.fossure.domain.helper.DifferenceView;
import io.github.fossure.domain.helper.Upload;
import io.github.fossure.domain.statistics.CountOccurrences;
import io.github.fossure.domain.statistics.ProjectOverview;
import io.github.fossure.domain.statistics.ProjectStatistic;
import io.github.fossure.repository.ProjectRepository;
import io.github.fossure.service.ProjectService;
import io.github.fossure.service.criteria.ProjectCriteria;
import io.github.fossure.service.criteria.query.ProjectQueryService;
import io.github.fossure.service.exceptions.*;
import io.github.fossure.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing {@link Project}.
 * API base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class ProjectResource {

    private static final Logger log = LoggerFactory.getLogger(ProjectResource.class);

    private static final String ENTITY_NAME = "project";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProjectService projectService;

    private final ProjectRepository projectRepository;

    private final ProjectQueryService projectQueryService;

    private final ApplicationProperties applicationProperties;

    public ProjectResource(
        ProjectService projectService,
        ProjectRepository projectRepository,
        ProjectQueryService projectQueryService,
        ApplicationProperties applicationProperties
    ) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.projectQueryService = projectQueryService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * {@code POST  /projects} : Create a new project.
     *
     * @param project the project to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new project,
     * or with status {@code 400 (Bad Request)} if the project has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/projects")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project) throws URISyntaxException {
        log.debug("REST request to save Project : {}", project);
        if (project.getId() != null) {
            throw new BadRequestAlertException("A new project cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Project result = projectService.save(project);
        return ResponseEntity
            .created(new URI("/api/v1/projects/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /projects/:id} : Updates an existing project.
     *
     * @param id      ID of the project to save.
     * @param project the project to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated project,
     * or with status {@code 400 (Bad Request)} if the project is not valid,
     * or with status {@code 500 (Internal Server Error)} if the project couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/projects/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Project> updateProject(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Project project
    ) throws URISyntaxException {
        log.debug("REST request to update Project : {}, {}", id, project);
        if (project.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, project.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Project result = projectService.save(project);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, project.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /projects/:id} : Partial updates given fields of an existing project, field will ignore if it is null.
     *
     * @param id      ID of the project to save
     * @param project the project to update
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated project,
     * or with status {@code 400 (Bad Request)} if the project is not valid,
     * or with status {@code 404 (Not Found)} if the project is not found,
     * or with status {@code 500 (Internal Server Error)} if the project couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/projects/{id}", consumes = "application/merge-patch+json")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Project> partialUpdateProject(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Project project
    ) throws URISyntaxException {
        log.debug("REST request to partial update Project partially : {}, {}", id, project);
        if (project.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, project.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Project> result = projectService.partialUpdate(project);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, project.getId().toString())
        );
    }

    /**
     * {@code GET  /projects} : get all the projects.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of projects in body.
     */
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects(
        ProjectCriteria criteria,
        @org.springdoc.api.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get Projects by criteria: {}", criteria);
        Page<Project> page = projectQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /projects/count} : count all the projects.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/projects/count")
    public ResponseEntity<Long> countProjects(ProjectCriteria criteria) {
        log.debug("REST request to count Projects by criteria: {}", criteria);
        return ResponseEntity.ok().body(projectQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /projects/:id} : get the "id" project.
     *
     * @param id the id of the project to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the project, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        log.debug("REST request to get Project : {}", id);
        Optional<Project> project = projectService.findOne(id);
        return ResponseUtil.wrapOrNotFound(project);
    }

    /**
     * {@code DELETE  /projects/:id} : delete a specific project.
     *
     * @param id ID of the project to delete
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/projects/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.debug("REST request to delete Project : {}", id);
        projectService.delete(id);

        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET  /projects/:id/oss} : Get an OSS list for a project.
     *
     * @param id     ID of the project
     * @param format file format of the OSS list. {@link ExportFormat}
     * @param type   content type of the OSS list. {@link OssType}
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the OSS list as a {@link File},
     * or with status {@code 400 (Bad Request)} if the project id, format or type are not valid.
     */
    @GetMapping("/projects/{id}/oss")
    public ResponseEntity<File> getOssList(
        @PathVariable Long id,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "type", required = false) String type
    ) {
        log.debug("REST request to download a OSS list of a project");
        ExportFormat exportFormat = ExportFormat.CSV;
        OssType ossType = OssType.DEFAULT;

        // Convert format and type from a string to the specific object
        if (format != null) exportFormat = ExportFormat.getExportFormatByValue(format.toLowerCase());
        if (type != null) ossType = OssType.getOssTypeByValue(type.toLowerCase());

        if (exportFormat == null) throw new BadRequestAlertException("Unsupported OSS list format", ENTITY_NAME, "osserror");
        if (ossType == null) throw new BadRequestAlertException("Unsupported OSS list type", ENTITY_NAME, "osserror");

        Optional<Project> project = projectService.findOne(id);

        if (project.isPresent()) {
            try {
                File file = projectService.createOssList(project.get(), exportFormat, ossType);
                return ResponseEntity.ok(file);
            } catch (ExportException e) {
                throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "idinvalid");
            }
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }
    }

    /**
     * {@code GET  /projects/:id/zip} : Download the license text archive for a project.
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the license text archive as a {@link File},
     * or with status {@code 400 (Bad Request)} if the project id, format or type are not valid.
     */
    @GetMapping("/projects/{id}/zip")
    public ResponseEntity<File> getZip(@PathVariable Long id) {
        log.debug("REST request to download a license text archive for project : {}", id);
        Optional<Project> optionalProject = projectService.findOne(id);

        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();
            File zipFile;
            String fileName = project.getName().replace(" ", "_") + "_" + project.getVersion() + ".zip";

            try {
                zipFile = new File(fileName, projectService.createLicenseZip(project), Constants.MIME_ZIP);
            } catch (FileNotFoundException e) {
                log.error("Cannot create license ZIP archive for project ID {} : {}", project.getId(), e.getMessage());
                throw new BadRequestAlertException("Cannot create license ZIP archive", ENTITY_NAME, "ziperror");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=\"" + fileName + "\"");
            return ResponseEntity.ok().headers(headers).body(zipFile);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }
    }

    /**
     * TODO create badges per project
     * {@code GET  /projects/:id/badge} : Get a badge with the risk information of a project.
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the badge as SVG,
     * or with status {@code 400 (Bad Request)} if the project ID is not valid
     */
    @GetMapping("/projects/{id}/badge")
    public ResponseEntity<String> getProjectBadge(@PathVariable Long id) {
        log.debug("REST request create a badge for project : {}", id);
        throw new BadRequestAlertException("Not implemented", ENTITY_NAME, "notimplemented");
    }

    /**
     * {@code GET  /projects/:id/overview} : Get an overview of a specific project.
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the overview,
     * or with status {@code 400 (Bad Request)} if the project is not valid
     */
    @GetMapping("/projects/{id}/overview")
    public ResponseEntity<ProjectOverview> getProjectOverview(@PathVariable Long id) {
        log.debug("REST request to get an overview for project : {}", id);
        Optional<Project> optionalProject = projectService.findOne(id);
        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();
            ProjectOverview projectOverview = projectService.getProjectOverview(project);

            return ResponseEntity.ok(projectOverview);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }
    }

    /**
     * {@code GET  /projects/:id/statistic} : Get statistic information of a specific project.
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the statistic,
     * or with status {@code 400 (Bad Request)} if the project is not valid
     */
    @GetMapping("/projects/{id}/statistic")
    public ResponseEntity<ProjectStatistic> getProjectStatistic(@PathVariable Long id) {
        log.debug("REST request to get statistics for project : {}", id);
        Optional<Project> optionalProject = projectService.findOne(id);
        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();
            ProjectStatistic projectStatistic = projectService.getProjectStatistic(project);

            return ResponseEntity.ok(projectStatistic);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }
    }

    /**
     * {@code GET  /projects/:id/risk} : Get the risk information of a project.
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the risk,
     * or with status {@code 400 (Bad Request)} if the project is not valid.
     */
    @GetMapping("/projects/{id}/risk")
    public ResponseEntity<List<CountOccurrences>> getProjectRisk(@PathVariable Long id) {
        log.debug("REST request to get the risk information of project : {}", id);
        Optional<Project> optionalProject = projectService.findOne(id);
        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();
            List<CountOccurrences> risks = projectService.getProjectRisk(project);

            return ResponseEntity.ok(risks);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }
    }

    /**
     * {@code GET  /projects/:id/licenses} : Get the distribution of all licenses of a project.
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the licenses,
     * or with status {@code 400 (Bad Request)} if the project is not valid.
     */
    @GetMapping("/projects/{id}/licenses")
    public ResponseEntity<List<CountOccurrences>> getProjectLicenses(@PathVariable Long id) {
        log.debug("REST request to get the distribution of all licenses of project : {}", id);
        Optional<Project> optionalProject = projectService.findOne(id);
        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();

            return ResponseEntity.ok(projectService.getProjectLicenses(project));
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }
    }

    /**
     * {@code GET  /projects/:id/create-archive} : Create a 3rd-party source code archive for a project.
     * Download it or export it to an external plattform.
     *
     * @param id       ID of the project
     * @param format   format of the archive. {@link ArchiveFormat}
     * @param shipment type of shipment method. {@link ShipmentMethod}
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the {@link File} with the archive,
     * or with status {@code 400 (Bad Request)} if the project, format or shipment is not valid
     * or the archive could not be created.
     */
    @GetMapping("/projects/{id}/create-archive")
    public ResponseEntity<File> createArchive(
        @PathVariable Long id,
        @RequestParam(value = "format") String format,
        @RequestParam(value = "shipment") String shipment
    ) {
        log.debug("REST request to create an 3rd-party source code archive for project : {}", id);

        ArchiveFormat archiveFormat = ArchiveFormat.FULL;
        ShipmentMethod shipmentMethod = ShipmentMethod.DOWNLOAD;
        if (format != null) archiveFormat = ArchiveFormat.getArchiveFormatByValue(format.toLowerCase());
        if (shipment != null) shipmentMethod = ShipmentMethod.getShipmentMethodByValue(shipment.toLowerCase());

        if (archiveFormat == null) throw new BadRequestAlertException("Unsupported archive format", ENTITY_NAME, "archiveerror");
        if (shipmentMethod == null) throw new BadRequestAlertException("Unsupported shipment method", ENTITY_NAME, "archiveerror");
        Optional<Project> optionalProject = projectService.findOne(id);
        if (optionalProject.isEmpty()) {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idinvalid");
        }

        var project = optionalProject.get();
        Map<File, Boolean> resultMap;
        File archiveFile;
        boolean complete;
        var headers = new HttpHeaders();
        try {
            resultMap = projectService.createArchive(project, archiveFormat, shipmentMethod);
            if (resultMap.keySet().stream().findFirst().isPresent()) {
                archiveFile = resultMap.keySet().stream().findFirst().get();
                complete = resultMap.get(archiveFile);
            } else {
                throw new BadRequestAlertException("Could not create the archive.", ENTITY_NAME, "archiveerror");
            }
        } catch (StorageException | RemoteRepositoryException | ZIPException | UploadException | FileNotFoundException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "archiveerror");
        }

        if (!complete) {
            headers.add("COMPLETE", "Archive is not complete. Please check archive log.");
        }

        if (shipmentMethod.equals(ShipmentMethod.EXPORT)) {
            headers.add(
                "PLATFORM",
                "Successfully exported archive " +
                archiveFile.getFileName() +
                " to " +
                applicationProperties.getSourceCodeArchive().getUploadPlatform()
            );
            return ResponseEntity.noContent().headers(headers).build();
        } else {
            headers.add(
                "Content-Disposition",
                "attachment; filename=\"" + archiveFile.getFileName() + "\"; filename*=\"" + archiveFile.getFileName() + "\""
            );
            return ResponseEntity.ok().headers(headers).body(archiveFile);
        }
    }

    /**
     * {@code POST  /projects/:id/create-next-version} : Create a project with a new version based on another project.
     *
     * @param id ID of the base project.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new project,
     * or with status {@code 400 (Bad Request)} if the base project in invalid.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/projects/{id}/create-next-version")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Project> createNextProject(
        @PathVariable final Long id,
        @RequestParam(value = "version") String version,
        @RequestParam(value = "delivered", required = false, defaultValue = "true") Boolean delivered,
        @RequestParam(value = "copy", required = false, defaultValue = "false") Boolean copying
    ) throws URISyntaxException {
        log.debug("REST request to create next project version from project : {}", id);
        if (id == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        Optional<Project> optionalProject = projectService.findOne(id);

        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();
            Project result = projectService.createNextVersion(project, version, delivered, copying);

            return ResponseEntity
                .created(new URI("/api/v1/projects/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                .body(result);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idnotfound");
        }


    }

    /**
     * {@code POST  /projects/:id/add-libraries} : Add manually libraries to a project.
     *
     * @param id        ID of the project
     * @param libraries list of libraries to be added
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)},
     * or with status {@code 400 (Bad Request)} if the project does not exist.
     */
    @PostMapping("/projects/{id}/add-libraries")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> addLibraries(@PathVariable Long id, @RequestBody List<Library> libraries) {
        log.debug("REST request to add libraries to project : {}", id);
        Optional<Project> optionalProject = projectService.findOne(id);

        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();
            projectService.addLibraries(project, libraries);

            return ResponseEntity
                .noContent()
                .headers(HeaderUtil.createAlert(applicationName, "Libraries have been added successfully", ENTITY_NAME))
                .build();
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idnotfound");
        }
    }

    /**
     * {@code POST  /projects/:id/upload} : Upload of a BOM or archive to a project.
     *
     * @param id     ID of the project
     * @param delete true if the libraries from a previous upload should be deleted, or
     *               false if the libraries from the new upload should be added to the previous results
     * @param upload The Upload object with libraries
     * @return The {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}, if the processing is successfully started,
     * or with status {@code 400 (Bad Request)} if an error occurred during processing or the project does not exist.
     */
    @PostMapping("/projects/{id}/upload")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> upload(
        @PathVariable Long id,
        @RequestParam(value = "delete", defaultValue = "true") boolean delete,
        @RequestBody Upload upload
    ) {
        log.debug("REST request with upload to project : {}", id);
        if (!projectService.existsById(id)) {
            throw new BadRequestAlertException("Project not found", ENTITY_NAME, "idnotfound");
        }

        Project project;
        Optional<Project> optionalProject = projectService.findOne(id);

        if (optionalProject.isPresent()) {
            project = optionalProject.get();

            project.setUploadState(UploadState.PROCESSING);
            projectService.saveAndFlush(project);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idnotfound");
        }

        try {
            projectService.processUpload(project, upload, delete);
        } catch (UploadException e) {
            log.error("Error while processing the upload : {}", e.getMessage());
        }

        return ResponseEntity.noContent().headers(HeaderUtil.createAlert(applicationName, "Upload was successful", ENTITY_NAME)).build();
    }

    /**
     * {@code GET  /projects/:id/in-development-project} : Get the project that is "In Development".
     *
     * @param id ID of the project
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the project,
     * or with status {@code 400 (Bad Request)} if the project is not valid or
     * no unique project which is "In Development" could be found.
     */
    @GetMapping("/projects/{id}/in-development-project")
    public ResponseEntity<Project> getInDevelopmentProjectById(@PathVariable Long id) {
        Optional<Project> optionalProject = projectService.findOne(id);

        if (optionalProject.isPresent()) {
            Project project = optionalProject.get();

            try {
                return ResponseEntity.ok().body(projectService.getInDevelopmentProject(project));
            } catch (ProjectException e) {
                throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "noproject");
            }
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idnotfound");
        }
    }

    /**
     * {@code GET  /projects/compare} : Compare two project for differences.
     *
     * @param firstProjectId  First project ID
     * @param secondProjectId Second project ID
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the differences of the compared projects,
     * or with status {@code 400 (Bad Request)} if the project IDs are not valid.
     */
    @GetMapping("/projects/compare")
    public ResponseEntity<DifferenceView> compare(
        @RequestParam(value = "firstProjectId") Long firstProjectId,
        @RequestParam(value = "secondProjectId") Long secondProjectId
    ) {
        Optional<Project> optionalFirstProject = projectService.findOne(firstProjectId);
        Optional<Project> optionalSecondProject = projectService.findOne(secondProjectId);

        if (optionalFirstProject.isPresent() && optionalSecondProject.isPresent()) {
            Project firstProject = optionalFirstProject.get();
            Project secondProject = optionalSecondProject.get();

            return ResponseEntity.ok().body(projectService.compare(firstProject, secondProject));
        } else {
            throw new BadRequestAlertException("Cannot find Project", ENTITY_NAME, "idnotfound");
        }
    }

    /**
     * {@code GET  /projects/:id/upload-by-url} : Upload a BOM or archive from a URL.
     * It's possible to specify credentials for a basic authentication.
     *
     * @param id          ID of the project for the upload
     * @param url         URL of the file
     * @param delete      true if the libraries from a previous upload should be deleted, or
     *                    false if the libraries from the new upload should be added to the previous results
     * @param credentials The basic authentication credentials
     * @return The {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}, if the processing is successfully started,
     * or with status {@code 400 (Bad Request)} if an error occurred during processing or the project does not exist.
     */
    @PostMapping("/projects/{id}/upload-by-url")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> uploadByUrl(
        @PathVariable Long id,
        @RequestParam(value = "url") String url,
        @RequestParam(value = "delete", defaultValue = "true") boolean delete,
        @RequestBody BasicAuthentication credentials
    ) {
        log.debug("REST request with upload to project by URL : {}", id);

        if (!projectService.existsById(id)) {
            throw new BadRequestAlertException("Project not found", ENTITY_NAME, "idnotfound");
        }

        Project project;
        Optional<Project> optionalProject = projectService.findOne(id);

        if (optionalProject.isPresent()) {
            project = optionalProject.get();

            project.setUploadState(UploadState.PROCESSING);
            projectService.saveAndFlush(project);
        } else {
            throw new BadRequestAlertException("Cannot find Project ID", ENTITY_NAME, "idnotfound");
        }

        try {
            String contentType = projectService.preCheckForUploadByUrl(project, url, credentials);
            projectService.processUploadByUrl(project, url, credentials, delete, contentType);
        } catch (UploadException e) {
            log.error("Error while downloading file : {}", e.getMessage());
            project.setUploadState(UploadState.FAILURE);
            projectService.saveAndFlush(project);
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "uploaderror");
        }

        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createAlert(applicationName, "Downloading and processing file", ENTITY_NAME))
            .build();
    }
}
