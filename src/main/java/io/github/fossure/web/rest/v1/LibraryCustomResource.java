package io.github.fossure.web.rest.v1;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.github.fossure.web.rest.errors.BadRequestAlertException;
import io.github.fossure.domain.File;
import io.github.fossure.domain.Fossology;
import io.github.fossure.domain.Library;
import io.github.fossure.domain.enumeration.ExportFormat;
import io.github.fossure.domain.helper.Copyright;
import io.github.fossure.repository.LibraryCustomRepository;
import io.github.fossure.service.LibraryCustomService;
import io.github.fossure.service.LibraryQueryCustomService;
import io.github.fossure.service.exceptions.ArchiveException;
import io.github.fossure.service.exceptions.ExportException;
import io.github.fossure.service.exceptions.LibraryException;
import io.github.fossure.web.rest.LibraryResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

/**
 * Custom REST controller for managing {@link Library}
 * where changes are inserted that are different from the base class generated by the JHipster generator.<br>
 * API base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class LibraryCustomResource extends LibraryResource {

    private static final Logger log = LoggerFactory.getLogger(LibraryCustomResource.class);

    private static final String ENTITY_NAME = "library";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LibraryCustomService libraryService;

    private final LibraryCustomRepository libraryRepository;

    public LibraryCustomResource(
        LibraryCustomService libraryService,
        LibraryCustomRepository libraryRepository,
        LibraryQueryCustomService libraryQueryService
    ) {
        super(libraryService, libraryRepository, libraryQueryService);
        this.libraryService = libraryService;
        this.libraryRepository = libraryRepository;
    }

    /**
     * {@code POST  /libraries} : Create a new library.
     *
     * @param library the library to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new library, or with status {@code 400 (Bad Request)} if the library has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/libraries")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Library> createLibrary(@Valid @RequestBody Library library) throws URISyntaxException {
        log.debug("REST request to save Library : {}", library);
        if (library.getId() != null) {
            throw new BadRequestAlertException("A new library cannot already have an ID", ENTITY_NAME, "idexists");
        }
        try {
            Library result = libraryService.saveWithCheck(library);
            return ResponseEntity
                .created(new URI("/api/libraries/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                .body(result);
        } catch (LibraryException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "libraryerror");
        }
    }

    /**
     * {@code PUT  /libraries/:id} : Updates an existing library.
     *
     * @param id      the id of the library to save.
     * @param library the library to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated library,
     * or with status {@code 400 (Bad Request)} if the library is not valid,
     * or with status {@code 500 (Internal Server Error)} if the library couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/libraries/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Library> updateLibrary(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Library library
    ) throws URISyntaxException {
        log.debug("REST request to update Library : {}, {}", id, library);
        if (library.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, library.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        try {
            Library result = libraryService.saveWithCheck(library);

            return ResponseEntity
                .ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, library.getId().toString()))
                .body(result);
        } catch (LibraryException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "libraryerror");
        }
    }

    /**
     * {@code PATCH  /libraries/:id} : Partial updates given fields of an existing library, field will ignore if it is null
     *
     * @param id      the id of the library to save.
     * @param library the library to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated library,
     * or with status {@code 400 (Bad Request)} if the library is not valid,
     * or with status {@code 404 (Not Found)} if the library is not found,
     * or with status {@code 500 (Internal Server Error)} if the library couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/libraries/{id}", consumes = "application/merge-patch+json")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Library> partialUpdateLibrary(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Library library
    ) throws URISyntaxException {
        return super.partialUpdateLibrary(id, library);
    }

    /**
     * {@code GET /libraries/export} : Export all library entries.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body a {@link File} with libraries,
     * or with status {@code 400 (Bad Request)} if libraries could not be serialized.
     */
    @GetMapping("/libraries/export")
    public ResponseEntity<File> exportLibraries(@RequestParam(value = "format", required = false) String format) {
        log.debug("REST request to export library table as : {}", format);
        ExportFormat exportFormat = ExportFormat.getExportFormatByValue(format.toLowerCase());

        if (exportFormat == null) throw new BadRequestAlertException("Unsupported export format", ENTITY_NAME, "exporterror");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"libraries.json\"; filename*=\"libraries.json\"");
            return ResponseEntity.ok().headers(headers).body(libraryService.export(exportFormat));
        } catch (ExportException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "exporterror");
        }
    }

    /**
     * {@code DELETE  /libraries/:id} : delete the "id" library.
     *
     * @param id the id of the library to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/libraries/{id}")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> deleteLibrary(@PathVariable Long id) {
        return super.deleteLibrary(id);
    }

    /**
     * {@code GET  /libraries/:id/analyse-copyright} : Analyse the copyright for a library.
     *
     * @param id the id of the library to analyse the copyright
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the copyright(s)
     */
    @GetMapping("/libraries/{id}/analyse-copyright")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Copyright> analyseCopyright(@PathVariable Long id) {
        log.debug("REST request to analyse copyright for library : {}", id);
        Optional<Library> library = libraryService.findOne(id);

        if (library.isEmpty()) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idinvalid");
        }

        try {
            Copyright copyright = libraryService.getCopyright(library.get());
            return ResponseEntity.ok(copyright);
        } catch (ArchiveException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "copyrighterror");
        }
    }

    /**
     * {@code GET  /libraries/:id/analyse-with-fossology} : Analyse a library with Fossology.
     *
     * @param id the id of the Library to analyse with Fossology
     * @return the {@link ResponseEntity} with status {@code 200 (OK)},
     * or with status {@code 400 (Bad Request)} if the library could not be analysed with Fossology.
     */
    @GetMapping("/libraries/{id}/analyse-with-fossology")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Void> analyseWithFossology(@PathVariable Long id) {
        log.debug("REST request to analyse Library with Fossology : {}", id);
        Optional<Library> library = libraryService.findOne(id);

        if (library.isEmpty()) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idinvalid");
        }

        try {
            libraryService.analyseWithFossology(library.get());
            return ResponseEntity.noContent().build();
        } catch (LibraryException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "fossologyerror");
        }
    }

    /**
     * {@code GET  /libraries/:id/fossology-analysis} : Get the information of a Fossology analysis.
     *
     * @param id the id of the Library
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the Fossology entity.
     */
    @GetMapping("/libraries/{id}/fossology-analysis")
    @Secured({ "ROLE_ADMIN", "ROLE_USER" })
    public ResponseEntity<Fossology> fossologyAnalysis(@PathVariable Long id) {
        log.debug("REST request to get the information of a Fossology analysis for Library : {}", id);
        Optional<Library> library = libraryService.findOne(id);

        if (library.isEmpty()) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idinvalid");
        }

        return ResponseEntity.ok(library.get().getFossology());
    }
}