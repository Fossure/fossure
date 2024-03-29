package io.github.fossure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fossure.config.Constants;
import io.github.fossure.domain.File;
import io.github.fossure.domain.Library;
import io.github.fossure.domain.License;
import io.github.fossure.domain.LicensePerLibrary;
import io.github.fossure.domain.enumeration.ExportFormat;
import io.github.fossure.domain.enumeration.LogSeverity;
import io.github.fossure.domain.helper.Copyright;
import io.github.fossure.repository.LibraryRepository;
import io.github.fossure.service.exceptions.*;
import io.github.fossure.service.helper.copyright.ArchiveHelper;
import io.github.fossure.service.helper.copyright.CopyrightAnalyser;
import io.github.fossure.service.helper.net.HttpHelper;
import io.github.fossure.service.helper.sourceCode.SourceCodeHelper;
import io.github.fossure.service.helper.urlparsing.LicenseURLparser;
import io.github.fossure.service.helper.urlparsing.SourceURLparser;
import io.github.fossure.service.pipeline.MavenLicenseStep;
import io.github.fossure.service.pipeline.NpmLicenseStep;
import io.github.fossure.service.pipeline.Pipeline;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing {@link Library}.
 */
@Service
@Transactional
public class LibraryService {

    private final Logger log = LoggerFactory.getLogger(LibraryService.class);

    private final LibraryRepository libraryRepository;

    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private final LicenseService licenseService;

    private final FossologyService fossologyService;

    private final SourceURLparser sourceURLparser;
    private final LicenseURLparser licenseURLparser;

    private final SourceCodeHelper sourceCodeHelper;

    public LibraryService(
        LibraryRepository libraryRepository,
        LicenseService licenseService,
        SourceCodeHelper sourceCodeHelper,
        ObjectMapper objectMapper,
        EntityManager entityManager,
        FossologyService fossologyService
    ) {
        this.libraryRepository = libraryRepository;
        this.licenseService = licenseService;
        this.sourceURLparser = new SourceURLparser();
        this.sourceURLparser.initCommands();
        this.licenseURLparser = new LicenseURLparser();
        this.licenseURLparser.initCommands();
        this.sourceCodeHelper = sourceCodeHelper;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.fossologyService = fossologyService;
    }

    /**
     * Save a library.
     *
     * @param library the entity to save.
     * @return the persisted entity.
     */
    public Library save(Library library) {
        log.debug("Request to save Library : {}", library);
        return libraryRepository.save(library);
    }

    /**
     * Save a library. Executes check on the library for validity and optimize the missing information.
     *
     * @param library the entity to save.
     * @return the persisted entity.
     * @throws LibraryException if the library already exists in the database.
     */
    public Library saveWithCheck(Library library) throws LibraryException {
        log.debug("Request to save Library with check : {}", library);
        /*
        If ID is null, library is a new Object
        If ID is not null, library Object gets updated
        */
        if (library.getId() == null) {
            Optional<Library> libraryOptional = findNamespaceNameVersion(
                library.getNamespace(),
                library.getName(),
                library.getVersion()
            );

            if (libraryOptional.isPresent()) {
                throw new LibraryAlreadyExistException(
                    "Library with Namespace : " +
                    library.getNamespace() +
                    ", Name : " +
                    library.getName() +
                    ", Version : " +
                    library.getVersion() +
                    " already exists",
                    libraryOptional.get()
                );
            }

            library.setCreatedDate(LocalDate.now());
        } else {
            Optional<Library> optionalLibrary = findOne(library.getId());

            if (optionalLibrary.isPresent()) {
                Library libraryInDb = optionalLibrary.get();

                if (
                    (
                        !libraryInDb.getNamespace().equals(library.getNamespace()) ||
                        !libraryInDb.getName().equals(library.getName()) ||
                        !libraryInDb.getVersion().equals(library.getVersion())
                    ) &&
                    findNamespaceNameVersion(library.getNamespace(), library.getName(), library.getVersion()).isPresent()
                ) {
                    throw new LibraryAlreadyExistException("Namespace, Name and Version can't be changed. This library already exists");
                }
            } else {
                library.setId(null);
                saveWithCheck(library);
            }
        }

        if (!library.validateLinkedLicenses()) throw new LibraryException("License selection is not valid");

        library = new Pipeline<>(new MavenLicenseStep()).pipe(new NpmLicenseStep()).execute(library);

        // TODO Add all autocompletion methods to Pipeline
        licenseAutocomplete(library);
        // TODO Improve incompatible license check (too slow)
        //hasIncompatibleLicenses(library);
        removeGenericLicenseUrl(library);
        urlAutocomplete(library);
        licenseTextAutocomplete(library);
        copyrightAutocomplete(library);
        calculateLibraryRisk(library);

        return save(library);
    }

    /**
     * Save and flush a Library without validating if a Library already exist.
     *
     * @param library the entity to save.
     * @return the persisted entity.
     */
    public Library saveAndFlush(Library library) {
        log.debug("Request to save and flush Library : {}", library);

        Library savedLibrary = save(library);
        libraryRepository.flush();

        return savedLibrary;
    }

    /**
     * Partially update a library.
     *
     * @param library the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Library> partialUpdate(Library library) {
        log.debug("Request to partially update Library : {}", library);

        return libraryRepository
            .findById(library.getId())
            .map(existingLibrary -> {
                if (library.getNamespace() != null) {
                    existingLibrary.setNamespace(library.getNamespace());
                }
                if (library.getName() != null) {
                    existingLibrary.setName(library.getName());
                }
                if (library.getVersion() != null) {
                    existingLibrary.setVersion(library.getVersion());
                }
                if (library.getType() != null) {
                    existingLibrary.setType(library.getType());
                }
                if (library.getOriginalLicense() != null) {
                    existingLibrary.setOriginalLicense(library.getOriginalLicense());
                }
                if (library.getLicenseUrl() != null) {
                    existingLibrary.setLicenseUrl(library.getLicenseUrl());
                }
                if (library.getLicenseText() != null) {
                    existingLibrary.setLicenseText(library.getLicenseText());
                }
                if (library.getSourceCodeUrl() != null) {
                    existingLibrary.setSourceCodeUrl(library.getSourceCodeUrl());
                }
                if (library.getpUrl() != null) {
                    existingLibrary.setpUrl(library.getpUrl());
                }
                if (library.getCopyright() != null) {
                    existingLibrary.setCopyright(library.getCopyright());
                }
                if (library.getCompliance() != null) {
                    existingLibrary.setCompliance(library.getCompliance());
                }
                if (library.getComplianceComment() != null) {
                    existingLibrary.setComplianceComment(library.getComplianceComment());
                }
                if (library.getComment() != null) {
                    existingLibrary.setComment(library.getComment());
                }
                if (library.getReviewed() != null) {
                    existingLibrary.setReviewed(library.getReviewed());
                }
                if (library.getLastReviewedDate() != null) {
                    existingLibrary.setLastReviewedDate(library.getLastReviewedDate());
                }
                if (library.getReviewedDeepScan() != null) {
                    existingLibrary.setReviewedDeepScan(library.getReviewedDeepScan());
                }
                if (library.getLastReviewedDeepScanDate() != null) {
                    existingLibrary.setLastReviewedDeepScanDate(library.getLastReviewedDeepScanDate());
                }
                if (library.getCreatedDate() != null) {
                    existingLibrary.setCreatedDate(library.getCreatedDate());
                }
                if (library.getHideForPublishing() != null) {
                    existingLibrary.setHideForPublishing(library.getHideForPublishing());
                }
                if (library.getMd5() != null) {
                    existingLibrary.setMd5(library.getMd5());
                }
                if (library.getSha1() != null) {
                    existingLibrary.setSha1(library.getSha1());
                }

                return existingLibrary;
            })
            .map(libraryRepository::save);
    }

    /**
     * Get all the libraries.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Library> findAll(Pageable pageable) {
        log.debug("Request to get all Libraries");
        return libraryRepository.findAll(pageable);
    }

    /**
     * Get all the libraries with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<Library> findAllWithEagerRelationships(Pageable pageable) {
        return libraryRepository.findAllWithEagerRelationships(pageable);
    }

    /**
     * Get one library by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Library> findOne(Long id) {
        log.debug("Request to get Library : {}", id);
        return libraryRepository.findOneWithEagerRelationships(id);
    }

    /**
     * Count all the libraries.
     * @return the number of libraries.
    */
    public long count() {
        return libraryRepository.count();
    }

    /**
     * Delete the library by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Library : {}", id);
        libraryRepository.deleteById(id);
    }

    /**
     * Count all the libraries with has an unidentified license (Unknown or Non-Licensed).
     *
     * @param unknown the unknown license.
     * @param nonLicensed the non-licensed license.
     * @return the number of libraries.
     */
    public long countByUnidentifiedLicense(License unknown, License nonLicensed) {
        return libraryRepository.countByUnidentifiedLicense(unknown, nonLicensed);
    }

    /**
     * Count all the libraries with a specific license.
     *
     * @param license the license to search for.
     * @return the number of libraries.
     */
    public long countByLicenseToPublishContainsLicense(License license) {
        return libraryRepository.countByLicenseToPublishContainsLicense(license);
    }
    /**
     * Get all the libraries with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<Library> findAllWithEagerLicenses(Pageable pageable) {
        long count = (long) entityManager.createQuery("select count(library.id) from Library library").getSingleResult();

        List<Long> libraryIds = entityManager
            .createQuery("select distinct library.id from Library library", Long.class)
            .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        List<Library> libraries = entityManager
            .createQuery(
                "select distinct library from Library library left join fetch library.licenseToPublishes left join fetch library.licenseOfFiles where library.id in (:libraryIds)",
                Library.class
            )
            .setParameter("libraryIds", libraryIds)
            .getResultList();

        return new PageImpl<>(libraries, pageable, count);
    }

    /**
     * Search for a Library with Namespace, Name and Version
     *
     * @param namespace    Namespace of a Library
     * @param name Name of a Library
     * @param version    Version of a Library
     * @return a Library as an Optional
     */
    public Optional<Library> findNamespaceNameVersion(String namespace, String name, String version) {
        log.debug("Request to get Library by Namespace : {} and Name : {} and Version : {}", namespace, name, version);
        return libraryRepository.findByNamespaceAndNameAndVersion(namespace, name, version);
    }

    /**
     * Search for a Library by MD5 or SHA1
     *
     * @param hash MD5 or SHA1 hash of a Library
     * @return a Library as an Optional
     */
    public Optional<Library> findByHash(String hash) {
        log.debug("Request to get Library by Hash : {}", hash);
        return libraryRepository.findByHash(hash).stream().findFirst();
    }

    /**
     * Autocomplete license set if the license set is empty.
     * Takes the originalLicense as an input.<br>
     * <b>Fields</b>: LinkedLicenses, LicenseToPublish
     *
     * @param library Library entity
     */
    public void licenseAutocomplete(Library library) {
        log.debug("Autocomplete of license fields");
        if (library.getOriginalLicense() == null) library.setOriginalLicense("");

        for (LicensePerLibrary lpl : library.getLicenses()) {
            Optional<License> optionalLicense = licenseService.findOne(lpl.getLicense().getId());
            optionalLicense.ifPresent(lpl::setLicense);
        }

        for (License license : library.getLicenseToPublishes()) {
            if (license.getLicenseRisk() == null) {
                Optional<License> optionalLicense = licenseService.findOne(license.getId());
                if (optionalLicense.isPresent()) {
                    license.setLicenseRisk(optionalLicense.get().getLicenseRisk());
                }
            }
        }

        if (
            library.getLicenses().isEmpty() ||
            (licenseService.unidentifiedLicense(library.getLicenses()) && !StringUtils.isBlank(library.getOriginalLicense()))
        ) {
            library.setLicenses(licenseService.findLicenseWithLinking(library.getOriginalLicense()));
        }

        // Bugfix for libraries where the license to publish is 'Unknown' but licenses are not 'Unknown'
        if (
            !library.getLicenses().isEmpty() &&
            (library.getLicenseToPublishes().size() == 1 && library.getLicenseToPublishes().contains(licenseService.getUnknownLicense()))
        ) {
            library.setLicenseToPublishes(licenseService.findLicenseToPublish(library.getLicenses()));
        }

        if (
            !library.getLicenses().isEmpty() &&
            (library.getLicenseToPublishes().isEmpty() || licenseService.unidentifiedLicense(library.getLicenseToPublishes()))
        ) {
            library.setLicenseToPublishes(licenseService.findLicenseToPublish(library.getLicenses()));
        }
    }

    public void hasIncompatibleLicenses(Library library) {
        log.debug(
            "Check if license combinations are incompatible for : [{} - {} - {}]",
            library.getNamespace(),
            library.getName(),
            library.getVersion()
        );

        // Check License To Publish for incompatibility
        log.info("First license to publish");
        if (!library.getLicenseToPublishes().isEmpty() && library.getLicenseToPublishes().size() > 1) {
            crossIncompatibilityCheckWithEveryLicense(library, library.getLicenseToPublishes(), library.getLicenseToPublishes());
        }

        // Check License Of Files for incompatibility
        log.info("second license of files");
        if (!library.getLicenseOfFiles().isEmpty() && library.getLicenseOfFiles().size() > 1) {
            crossIncompatibilityCheckWithEveryLicense(library, library.getLicenseOfFiles(), library.getLicenseOfFiles());
        }

        // Check if every license from License To Publish is incompatible with the licenses from License Of Files
        log.info("Third license to publish with license of files");
        if (!library.getLicenseToPublishes().isEmpty() && !library.getLicenseOfFiles().isEmpty()) {
            Set<License> publishFilesMerged = new HashSet<>(2);
            publishFilesMerged.addAll(library.getLicenseToPublishes());
            publishFilesMerged.addAll(library.getLicenseOfFiles());
            crossIncompatibilityCheckWithEveryLicense(library, publishFilesMerged, publishFilesMerged);
        }
    }

    private void crossIncompatibilityCheckWithEveryLicense(Library library, Set<License> firstSet, Set<License> secondSet) {
        log.debug("Check cross incompatibility with every license");
        final String licenseConflictErrorIssue = "License Conflict";
        final String licenseConflictErrorMessage = "License {0} is incompatible with license {1}.";

        for (License firstLicense : firstSet) {
            for (License secondLicense : secondSet) {
                if (
                    !library.containsErrorLogByMessage(
                        licenseConflictErrorIssue +
                        " - " +
                        MessageFormat.format(
                            licenseConflictErrorMessage,
                            firstLicense.getShortIdentifier(),
                            secondLicense.getShortIdentifier()
                        )
                    )
                ) {
                    List<License> firstAndSecondLicense = new ArrayList<>(2);
                    firstAndSecondLicense.add(firstLicense);
                    firstAndSecondLicense.add(secondLicense);
                    boolean isIncompatible = licenseService.checkForLicenseIncompatibility(firstAndSecondLicense);

                    if (isIncompatible) library.addErrorLog(
                        licenseConflictErrorIssue,
                        MessageFormat.format(
                            licenseConflictErrorMessage,
                            firstLicense.getShortIdentifier(),
                            secondLicense.getShortIdentifier()
                        ),
                        LogSeverity.HIGH
                    );
                }
            }
        }
    }

    public void reevaluateIncompatibleLicenses(License license) {
        log.debug("Start reevaluating incompatible licenses : {}", license.getShortIdentifier());

        List<Library> libraries = libraryRepository.findAllByLicenseToPublishAndLicenseOfFiles(license);
        log.debug("Number of Libraries to check for incompatibility : {}", libraries.size());
        libraries.forEach(this::hasIncompatibleLicenses);
        libraryRepository.saveAll(libraries);
        log.debug("Finished!");
    }

    /**
     * Autocomplete of source code and license URL field.
     *
     * @param library Library entity
     */
    public void urlAutocomplete(Library library) {
        log.debug("Autocomplete of source code and license URL");
        sourceCodeUrlBuilder(library);
        licenseUrlBuilder(library);
    }

    /**
     * Creates the source code URL from purl or Namespace, Name, Version and Type.
     *
     * @param library Library entity
     */
    public void sourceCodeUrlBuilder(Library library) {
        log.debug("Creating source code URL");
        if (StringUtils.isBlank(library.getSourceCodeUrl())) {
            try {
                if (!StringUtils.isBlank(library.getpUrl())) {
                    library.setSourceCodeUrl(sourceURLparser.getURL(library.getpUrl()));
                } else if (library.getType() != null) {
                    library.setSourceCodeUrl(
                        sourceURLparser.getURL(
                            library.getType().getValue(),
                            library.getNamespace(),
                            library.getName(),
                            library.getVersion()
                        )
                    );
                }
            } catch (GithubRateLimitException e) {
                log.info(
                    "Github request limit reached. Library : Namespace : {} - Name : {} - Version : {}",
                    library.getNamespace(),
                    library.getName(),
                    library.getVersion()
                );
                library.setSourceCodeUrl(Constants.GITHUB_LIMIT);
            } catch (IOException | ParseException | ClassCastException | InterruptedException e) {
                log.info(
                    "Source Code URL not available. Library : Namespace : {} - Name : {} - Version : {}",
                    library.getNamespace(),
                    library.getName(),
                    library.getVersion()
                );
                library.setSourceCodeUrl(Constants.NO_URL);
            }
        }
    }

    /**
     * Creates the license URL from purl or Namespace, Name, Version and Type.
     *
     * @param library Library entity
     */
    public void licenseUrlBuilder(Library library) {
        log.debug("Creating license URL");
        if (StringUtils.isBlank(library.getLicenseUrl())) {
            try {
                if (!StringUtils.isBlank(library.getpUrl())) {
                    library.setLicenseUrl(licenseURLparser.getURL(library.getpUrl()));
                } else if (library.getType() != null) {
                    library.setLicenseUrl(
                        licenseURLparser.getURL(
                            library.getType().getValue(),
                            library.getNamespace(),
                            library.getName(),
                            library.getVersion()
                        )
                    );
                }
            } catch (GithubRateLimitException e) {
                log.info(
                    "Github request limit reached. Library : Namespace : {} - Name : {} - Version : {}",
                    library.getNamespace(),
                    library.getName(),
                    library.getVersion()
                );
                library.setLicenseUrl(Constants.GITHUB_LIMIT);
            } catch (IOException | ParseException | ClassCastException | InterruptedException e) {
                log.info(
                    "License URL not available. Library : Namespace : {} - Name : {} - Version : {}",
                    library.getNamespace(),
                    library.getName(),
                    library.getVersion()
                );
                library.setLicenseUrl(Constants.NO_URL);
            }
        }
    }

    /**
     * Removes the license URL from a Library if it is a generic license URL.
     *
     * @param library Library entity
     */
    public void removeGenericLicenseUrl(Library library) {
        log.debug("Removing license URL from Library if generic");
        // Check if license URL is a generic url
        if (!StringUtils.isBlank(library.getLicenseUrl())) {
            List<String> licenseUrls = licenseService.cleansedLicenseUrl(library.getLicenseUrl());

            if (!licenseUrls.isEmpty()) {
                library.setLicenseUrl(String.join(", ", licenseUrls));
            } else {
                library.setLicenseUrl(null);
            }
        }
    }

    /**
     * Autocomplete of the license text field.
     * Downloads all URLs and concatenates them into one text.
     * The texts are delimited with a separator.
     *
     * @param library Library entity
     */
    public void licenseTextAutocomplete(Library library) {
        log.debug("Autocomplete of license text");
        if (
            !StringUtils.isBlank(library.getLicenseUrl()) &&
            !library.getLicenseUrl().equals(Constants.NO_URL) &&
            StringUtils.isBlank(library.getLicenseText())
        ) {
            log.info("Downloading license URL(s) : {}", library.getLicenseUrl());
            StringBuilder licenseTextBuilder = new StringBuilder();
            boolean multipleLicenseTexts = false;
            final String textSeparator = Constants.LICENSE_TEXT_SEPARATOR;

            String[] urls = library.getLicenseUrl().split(",");
            if (urls.length == 0) urls = new String[] { library.getLicenseUrl() };

            for (String url : urls) {
                url = url.trim();
                try {
                    String licenseText = licenseService.downloadLicenseText(url);

                    if (!licenseText.isEmpty()) {
                        if (multipleLicenseTexts) {
                            licenseTextBuilder.append(textSeparator).append(licenseText);
                        } else {
                            licenseTextBuilder.append(licenseText);
                            multipleLicenseTexts = true;
                        }
                    }
                } catch (URISyntaxException e) {
                    log.info(
                        "License text URL is not valid for Library [ {} - {} ] : {}",
                        library.getId(),
                        library.getLicenseUrl(),
                        e.getMessage()
                    );
                } catch (IOException | InterruptedException e) {
                    log.info("License text cannot be downloaded for Library [ {} ] : {}", library.getId(), e.getMessage());
                }
            }
            library.setLicenseText(licenseTextBuilder.toString());
        }
    }

    /**
     * Get the copyright information of the source code archive from a library.
     *
     * @param library Library entity
     * @return a Copyright with full and simple copyrights
     * @throws ArchiveException if the archive cannot be downloaded, read or analysed.
     */
    public Copyright getCopyright(Library library) throws ArchiveException {
        InputStream archiveInputStream = null;
        String label = SourceCodeHelper.createLabel(library);

        try {
            java.io.File index = SourceCodeHelper.getLocalIndexFile();
            String identifier = SourceCodeHelper.checkRepository(index, label);

            //if (identifier != null) archiveInputStream = HttpHelper.downloadResource(sourceCodeHelper.getRemoteArchivePath() + identifier);

            if (identifier == null) {
                if (library.sourceCodeUrlIsValid()) {
                    log.info(
                        "Library {} is not available in the 3rd-party repository. Trying to download the source code URL from the library.",
                        label
                    );
                    archiveInputStream = HttpHelper.downloadResource(library.getSourceCodeUrl());
                } else {
                    identifier = SourceCodeHelper.checkRepositoryWithFuzzySearch(index, label);

                    if (identifier != null) {
                        log.info("The source code archive was found using the fuzzy search : {}", identifier);

                        archiveInputStream = HttpHelper.downloadResource(sourceCodeHelper.getRemoteArchivePath() + identifier);
                    }
                }
            } else {
                log.info("Library {} is available in the 3rd-party repository and will be downloaded from there.", label);
                archiveInputStream = HttpHelper.downloadResource(sourceCodeHelper.getRemoteArchivePath() + identifier);
            }
        } catch (FileNotFoundException e) {
            log.info("File not found : {}", e.getMessage());
        } catch (IOException e) {
            log.info("Source code archive could not be downloaded from remote platform : {}", e.getMessage());
        }

        /*if (archiveInputStream == null && !StringUtils.isBlank(library.getSourceCodeUrl())) {
            try {
                archiveInputStream = HttpHelper.downloadResource(library.getSourceCodeUrl());
            } catch (IOException e) {
                log.info("Source code URL is not valid or cannot be downloaded for Library [ {} ] : {}", library.getId(), e.getMessage());
            }
        }*/

        if (archiveInputStream == null) {
            if (!StringUtils.isBlank(library.getLicenseText())) {
                Set<String> copyrights = CopyrightAnalyser.extractCopyright(library.getLicenseText());

                if (copyrights.size() > 0) {
                    log.debug("Only the license text was analysed for library [ {} ]", library.getId());
                    return new Copyright(new HashSet<>(0), copyrights);
                }
            }

            throw new ArchiveException("Source code archive cannot be analysed");
        }

        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(archiveInputStream);

            Copyright copyrights = ArchiveHelper.analyseForCopyright(bufferedInputStream, true);
            bufferedInputStream.close();

            return copyrights;
        } catch (ArchiveException e) {
            log.info("Archive compression or type is unknown or unsupported : {}", e.getMessage());
            throw new ArchiveException("Source code archive cannot be scanned. Archive type is not supported");
        } catch (IOException e) {
            log.error("Input stream cannot be reset : {}", e.getMessage());
            throw new ArchiveException("Error while scanning source code archive");
        }
    }

    /**
     * Autocomplete of the copyright field.
     * Analyses all files and "copyright" files like license, readme, notice etc.-<br>
     * If the difference of the results between the all and "copyright" files is too big, than an error will be logged.<br>
     * <b>Cases:</b>
     * <ul>
     *     <li>Full result is twice as big as "copyright", than copyright will be saved and an error is logged.</li>
     *     <li>Copyright result is empty, than full will be saved and an error is logged.</li>
     * </ul>
     *
     * @param library Library entity
     */
    public void copyrightAutocomplete(Library library) {
        log.debug("Autocomplete of copyright");
        if (StringUtils.isBlank(library.getCopyright())) {
            InputStream archiveInputStream = null;
            String label = SourceCodeHelper.createLabel(library);

            try {
                java.io.File index = SourceCodeHelper.getLocalIndexFile();
                String identifier = SourceCodeHelper.checkRepository(index, label);

                //if (identifier != null) archiveInputStream =
                //    HttpHelper.downloadResource(sourceCodeHelper.getRemoteArchivePath() + identifier);

                if (identifier == null) {
                    if (library.sourceCodeUrlIsValid()) {
                        log.info(
                            "Library {} is not available in the 3rd-party repository. Trying to download the source code URL from the library.",
                            label
                        );
                        archiveInputStream = HttpHelper.downloadResource(library.getSourceCodeUrl());
                    } else {
                        identifier = SourceCodeHelper.checkRepositoryWithFuzzySearch(index, label);

                        if (identifier != null) {
                            log.info("The source code archive was found using the fuzzy search : {}", identifier);
                            library.addErrorLog(
                                "Copyright",
                                "The source code archive was found using the fuzzy search." +
                                " The copyright analysis may not be correct and should be checked for validity : " +
                                identifier,
                                LogSeverity.MEDIUM
                            );

                            archiveInputStream = HttpHelper.downloadResource(sourceCodeHelper.getRemoteArchivePath() + identifier);
                        }
                    }
                } else {
                    log.info("Library {} is available in the 3rd-party repository and will be downloaded from there.", label);
                    archiveInputStream = HttpHelper.downloadResource(sourceCodeHelper.getRemoteArchivePath() + identifier);
                }
            } catch (FileNotFoundException e) {
                log.info("File not found : {}", e.getMessage());
            } catch (IOException e) {
                log.info("Source code archive could not be downloaded from remote platform : {}", e.getMessage());
            }

            /*            if (archiveInputStream == null && !StringUtils.isBlank(library.getSourceCodeUrl())) {
                try {
                    archiveInputStream = HttpHelper.downloadResource(library.getSourceCodeUrl());
                } catch (IOException e) {
                    log.info(
                        "Source code URL is not valid or cannot be downloaded for Library [ {} ] : {}",
                        library.getId(),
                        e.getMessage()
                    );
                }
            }*/

            if (archiveInputStream == null) {
                if (!StringUtils.isBlank(library.getLicenseText())) {
                    Set<String> copyrights = CopyrightAnalyser.extractCopyright(library.getLicenseText());

                    if (copyrights.size() > 0) {
                        log.debug("Only the license text was analysed for copyrights for library [ {} ]", library.getId());
                        library.addErrorLog(
                            "Copyright",
                            "Only the license text could be analysed. No source code archive was found, therefore possibly not all copyrights were found",
                            LogSeverity.LOW
                        );
                        library.setCopyright(String.join(System.lineSeparator(), copyrights));
                    } else {
                        library.setCopyright(Constants.NO_COPYRIGHT);
                        library.addErrorLog(
                            "Copyright",
                            "No copyrights found. No source code archive was found for the analysis and the license text does not contain copyrights",
                            LogSeverity.HIGH
                        );
                    }
                    return;
                }

                library.setCopyright(Constants.NO_COPYRIGHT);
                library.addErrorLog(
                    "Copyright",
                    "No source code archive or license text could be found for the analysis",
                    LogSeverity.HIGH
                );

                return;
            }

            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(archiveInputStream);

                Copyright copyrights = ArchiveHelper.analyseForCopyright(bufferedInputStream, true);
                bufferedInputStream.close();

                if (
                    copyrights.getSimpleCopyright().size() > 0 &&
                    copyrights.getFullCopyright().size() > copyrights.getSimpleCopyright().size() * 2
                ) {
                    library.addErrorLog(
                        "Copyright",
                        "The result between the analysis of all files and the documentation files is too large. The result must be audited manually",
                        LogSeverity.MEDIUM
                    );

                    library.setCopyright(String.join("\n", copyrights.getSimpleCopyright()));
                } else if (copyrights.getSimpleCopyright().size() == 0) {
                    if (copyrights.getFullCopyright().size() > 0) {
                        library.addErrorLog("Copyright", "Could not find any copyrights in documentation files", LogSeverity.LOW);
                        library.setCopyright(String.join("\n", copyrights.getFullCopyright()));
                    } else {
                        library.addErrorLog("Copyright", "No copyrights found", LogSeverity.HIGH);
                        library.setCopyright(Constants.NO_COPYRIGHT);
                    }
                } else {
                    library.setCopyright(String.join(System.lineSeparator(), copyrights.getSimpleCopyright()));
                }
            } catch (ArchiveException e) {
                log.info("Archive compression or type is unknown or unsupported : {}", e.getMessage());
                library.addErrorLog(
                    "Copyright",
                    "Source code archive could not be analysed. Archive type is not supported",
                    LogSeverity.HIGH
                );
                library.setCopyright(Constants.NO_COPYRIGHT);
            } catch (IOException e) {
                log.error("Input stream cannot be reset : {}", e.getMessage());
                library.addErrorLog("Copyright", "Source code archive could not be analysed. An error occurred", LogSeverity.HIGH);
                library.setCopyright(Constants.NO_COPYRIGHT);
            }
        }
    }

    public void analyseWithFossology(Library library) throws LibraryException {
        String label = SourceCodeHelper.createLabel(library);

        try {
            java.io.File index = SourceCodeHelper.getLocalIndexFile();
            String identifier = SourceCodeHelper.checkRepository(index, label);

            if (identifier == null) {
                if (library.sourceCodeUrlIsValid()) {
                    log.info(
                        "Library {} is not available in the 3rd-party repository. Trying to download the source code URL from the library.",
                        label
                    );
                    try {
                        fossologyService.upload(library, this, library.getSourceCodeUrl(), label);
                    } catch (Exception e) {
                        log.error(
                            "Source code archive of library [{} - {} - {}] could not be analysed with Fossology : {}",
                            library.getNamespace(),
                            library.getName(),
                            library.getVersion(),
                            e.getMessage()
                        );
                        throw new LibraryException("Library cannot be analysed with Fossology");
                    }
                } else {
                    identifier = SourceCodeHelper.checkRepositoryWithFuzzySearch(index, label);

                    if (identifier != null) {
                        log.info("The source code archive was found using the fuzzy search : {}", identifier);

                        try {
                            fossologyService.upload(library, this, sourceCodeHelper.getRemoteArchivePath() + identifier, label);
                        } catch (Exception e) {
                            log.error(
                                "Source code archive of library [{} - {} - {}] could not be analysed with Fossology : {}",
                                library.getNamespace(),
                                library.getName(),
                                library.getVersion(),
                                e.getMessage()
                            );
                            throw new LibraryException("Library cannot be analysed with Fossology");
                        }
                    }
                }
            } else {
                log.info("Library {} is available in the 3rd-party repository and will be downloaded from there.", label);
                try {
                    fossologyService.upload(library, this, sourceCodeHelper.getRemoteArchivePath() + identifier, label);
                } catch (Exception e) {
                    log.error(
                        "Source code archive of library [{} - {} - {}] could not be analysed with Fossology : {}",
                        library.getNamespace(),
                        library.getName(),
                        library.getVersion(),
                        e.getMessage()
                    );
                    throw new LibraryException("Library cannot be analysed with Fossology");
                }
            }
        } catch (FileNotFoundException e) {
            log.info("File not found : {}", e.getMessage());
            throw new LibraryException("No source code archive was found for the analysis with Fossology");
        } catch (IOException e) {
            log.info("Source code archive could not be downloaded from remote platform : {}", e.getMessage());
            throw new LibraryException("Source code archive cannot be downloaded from the remote platform for the analysis with Fossology");
        }
    }

    /**
     * Set for every license in a library the order ID. It iterates over the licenses Set of LicensePerLibrary.
     *
     * @param library A library entity
     * @throws LibraryException if the link type of the last license is not null.
     */
    public void setLicenseOrderId(Library library) throws LibraryException {
        log.debug("Setting the order ID for licenses of library : {}", library.getId());
        int orderCounter = 0;
        for (LicensePerLibrary lpl : library.getLicenses()) {
            lpl.setOrderId(orderCounter);

            if (library.getLicenses().size() - 1 == orderCounter && lpl.getLinkType() != null) {
                throw new LibraryException("The order of the license Set is wrong. Last license has not a 'null' link type");
            }

            orderCounter++;
        }
    }

    public void calculateLibraryRisk(Library library) {
        log.debug("Calculating library risk for library : {}", library.getId());
        library.setLibraryRisk(library.getLicenseRisk(library.getLicenseToPublishes()));
    }

    /**
     * Export all libraries as a JSON or CSV file.
     *
     * @param format Format of the export. {@link ExportFormat}
     * @return a file with libraries as JSON or CSV
     * @throws ExportException if the file could not be serialized or the export format is not supported.
     */
    public File export(ExportFormat format) throws ExportException {
        log.debug("Request to export libraries");
        final String baseFileName = "libraries";
        String fileName;

        switch (format) {
            case JSON:
                // List<Library> libraryPageTest = findAllWithEagerLicenses(PageRequest.of(0, 200, Sort.by(Sort.Order.asc("name")))).getContent();
                List<Library> libraries = libraryRepository.findAllWithEagerRelationships();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(33554432);
                try {
                    // outputStream.write(objectMapper.writeValueAsBytes(libraryPageTest));
                    outputStream.write(objectMapper.writeValueAsBytes(libraries));
                } catch (IOException e) {
                    log.error("Libraries could not be serialized to JSON : {}", e.getMessage());
                    throw new ExportException("Error while exporting libraries to JSON");
                }

                fileName = baseFileName + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".json";
                return new File(fileName, outputStream.toByteArray(), "application/json");
            case CSV:
                Page<Library> libraryPage = findAllWithEagerLicenses(PageRequest.of(0, 200, Sort.by(Sort.Order.asc("name"))));
                int totalPages = libraryPage.getTotalPages();
                int currentPage = 0;

                String[] headers = {
                    "Namespace",
                    "Name",
                    "Version",
                    "Type",
                    "OriginalLicense",
                    "Licenses",
                    "LicenseToPublish",
                    "LicenseOfFiles",
                    "LicenseUrl",
                    "SourceCodeUrl",
                    "purl",
                    "Copyright",
                    "Comment",
                    "Compliance",
                    "ComplianceComment",
                    "HideForPublishing",
                    "Reviewed",
                    "LastReviewedBy",
                    "LastReviewedDate",
                    "CreatedDate",
                };

                StringBuilder csvBuilder = new StringBuilder(33554432);
                CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(';').withHeader(headers);

                while (currentPage < totalPages) {
                    try {
                        if (currentPage == 1) csvFormat = CSVFormat.DEFAULT.withDelimiter(';');
                        CSVPrinter csvPrinter = new CSVPrinter(csvBuilder, csvFormat);

                        for (Library library : libraryPage.getContent()) {
                            final String licenses = library.printLinkedLicenses();
                            final String licenseToPublish = library
                                .getLicenseToPublishes()
                                .stream()
                                .map(License::getShortIdentifier)
                                .collect(Collectors.joining(", "));
                            final String licenseOfFiles = library
                                .getLicenseOfFiles()
                                .stream()
                                .map(License::getShortIdentifier)
                                .collect(Collectors.joining(", "));

                            csvPrinter.printRecord(
                                library.getNamespace(),
                                library.getName(),
                                library.getVersion(),
                                library.getType(),
                                library.getOriginalLicense(),
                                licenses,
                                licenseToPublish,
                                licenseOfFiles,
                                library.getLicenseUrl(),
                                library.getSourceCodeUrl(),
                                library.getpUrl(),
                                library.getCopyright(),
                                library.getComment(),
                                library.getCompliance(),
                                library.getComplianceComment(),
                                library.getHideForPublishing() ? "x" : "",
                                library.getReviewed() ? "x" : "",
                                library.getLastReviewedBy() != null ? library.getLastReviewedBy().getLogin() : "",
                                library.getLastReviewedDate() != null
                                    ? library.getLastReviewedDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    : "",
                                library.getCreatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            );
                        }
                        csvPrinter.flush();
                        currentPage++;
                        libraryPage = findAllWithEagerLicenses(PageRequest.of(currentPage, 200, Sort.by(Sort.Order.asc("name"))));
                    } catch (IOException e) {
                        log.error("Libraries could not be serialized to CSV");
                        throw new ExportException("Error while exporting libraries to CSV");
                    }
                }

                fileName = baseFileName + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".csv";
                return new File(fileName, csvBuilder.toString().getBytes(StandardCharsets.UTF_8), "text/csv");
            default:
                throw new ExportException("Unsupported export format : " + format.getValue());
        }
    }
}
