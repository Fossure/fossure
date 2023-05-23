package io.github.fossure.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fossure.config.ApplicationProperties;
import io.github.fossure.config.Constants;
import io.github.fossure.domain.*;
import io.github.fossure.domain.enumeration.*;
import io.github.fossure.domain.helper.BasicAuthentication;
import io.github.fossure.domain.helper.DifferenceView;
import io.github.fossure.domain.helper.Upload;
import io.github.fossure.domain.statistics.CountOccurrences;
import io.github.fossure.domain.statistics.ProjectOverview;
import io.github.fossure.domain.statistics.ProjectStatistic;
import io.github.fossure.domain.statistics.Series;
import io.github.fossure.repository.LicenseRiskRepository;
import io.github.fossure.repository.ProjectRepository;
import io.github.fossure.repository.RequirementRepository;
import io.github.fossure.repository.UserRepository;
import io.github.fossure.service.exceptions.*;
import io.github.fossure.service.helper.LicenseZipHelper;
import io.github.fossure.service.helper.OssListHelper;
import io.github.fossure.service.helper.net.HttpHelper;
import io.github.fossure.service.helper.sourceCode.SourceCodeHelper;
import io.github.fossure.service.pipeline.MavenLicenseStep;
import io.github.fossure.service.pipeline.NpmLicenseStep;
import io.github.fossure.service.pipeline.Pipeline;
import io.github.fossure.service.upload.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.persistence.EntityManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service implementation for managing {@link Project}.
 */
@Service
@Transactional
public class ProjectService {

    private final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final Logger archiveLog = LoggerFactory.getLogger("archive");

    private final ProjectRepository projectRepository;

    private final DependencyService dependencyService;

    private final LibraryService libraryService;

    private final RequirementRepository requirementRepository;

    private final LicenseRiskRepository licenseRiskRepository;

    private final UserRepository userRepository;

    private final ApplicationProperties applicationProperties;

    private final EntityManager entityManager;

    private final AssetManager<Library> assetManager;

    public ProjectService(
        ProjectRepository projectRepository,
        DependencyService dependencyService,
        RequirementRepository requirementRepository,
        LicenseRiskRepository licenseRiskRepository,
        LibraryService libraryService,
        ObjectMapper objectMapper,
        UserRepository userRepository,
        EntityManager entityManager,
        ApplicationProperties applicationProperties
    ) {
        this.projectRepository = projectRepository;
        this.dependencyService = dependencyService;
        this.libraryService = libraryService;
        this.requirementRepository = requirementRepository;
        this.licenseRiskRepository = licenseRiskRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.applicationProperties = applicationProperties;

        assetManager = new AssetManager<>();

        AssetLoader<Library> bomLoader = new BomLoader();
        AssetLoader<Library> csvLoader = new LibraryCsvLoader();
        AssetLoader<Library> archiveLoader = new ArchiveLoader(libraryService);

        assetManager.addLoader(bomLoader, "text/xml");
        assetManager.addLoader(bomLoader, "application/xml");
        assetManager.addLoader(csvLoader, "application/vnd.ms-excel");
        assetManager.addLoader(csvLoader, "text/csv");
        assetManager.addLoader(archiveLoader, "application/zip");
        assetManager.addLoader(archiveLoader, "application/x-zip-compressed");
        assetManager.addLoader(archiveLoader, "application/gzip");
        assetManager.addLoader(archiveLoader, "application/java-archive");
        assetManager.addLoader(archiveLoader, "application/x-tar");
        assetManager.addLoader(archiveLoader, "application/octet-stream");
        assetManager.addLoader(
            file -> {
                try {
                    return objectMapper.readValue(file.getFile(), new TypeReference<>() {
                    });
                } catch (IOException e) {
                    log.error("Error while parsing JSON file : {}", e.getMessage());
                    throw new UploadException("JSON file can't be read");
                }
            },
            "application/json"
        );
    }

    /**
     * Save a project.
     *
     * @param project the entity to save.
     * @return the persisted entity.
     */
    public Project save(Project project) {
        log.debug("Request to save Project : {}", project);
        return projectRepository.save(project);
    }

    /**
     * Save a project. Checks if the delivered field got changed to the set the delivered date.
     *
     * @param project the entity to save.
     * @return the persisted entity.
     */
    public Project saveWithCheck(Project project) {
        log.debug("Request to save Project : {}", project);
        /*
        If ID is null, project is a new Object
        If ID is not null, project Object gets updated.
         */
        if (project.getId() != null) {
            Optional<Project> projectInDb = findOne(project.getId());
            if (projectInDb.isPresent()) {
                if (project.getDelivered() && !projectInDb.get().getDelivered()) {
                    project.setDeliveredDate(Instant.now());
                } else if (!project.getDelivered() && projectInDb.get().getDelivered()) {
                    project.setDeliveredDate(null);
                }
            }
        }

        return projectRepository.save(project);
    }

    public Project saveAndFlush(Project project) {
        log.debug("Request to save and flush Project : {}", project);
        return projectRepository.saveAndFlush(project);
    }

    /**
     * Partially update a project.
     *
     * @param project the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Project> partialUpdate(Project project) {
        log.debug("Request to partially update Project : {}", project);

        return projectRepository
            .findById(project.getId())
            .map(existingProject -> {
                if (project.getName() != null) {
                    existingProject.setName(project.getName());
                }
                if (project.getLabel() != null) {
                    existingProject.setLabel(project.getLabel());
                }
                if (project.getVersion() != null) {
                    existingProject.setVersion(project.getVersion());
                }
                if (project.getCreatedDate() != null) {
                    existingProject.setCreatedDate(project.getCreatedDate());
                }
                if (project.getLastUpdatedDate() != null) {
                    existingProject.setLastUpdatedDate(project.getLastUpdatedDate());
                }
                if (project.getUploadState() != null) {
                    existingProject.setUploadState(project.getUploadState());
                }
                if (project.getDisclaimer() != null) {
                    existingProject.setDisclaimer(project.getDisclaimer());
                }
                if (project.getDelivered() != null) {
                    existingProject.setDelivered(project.getDelivered());
                }
                if (project.getDeliveredDate() != null) {
                    existingProject.setDeliveredDate(project.getDeliveredDate());
                }
                if (project.getContact() != null) {
                    existingProject.setContact(project.getContact());
                }
                if (project.getComment() != null) {
                    existingProject.setComment(project.getComment());
                }
                if (project.getPreviousProject() != null) {
                    existingProject.setPreviousProject(project.getPreviousProject());
                }
                if (project.getUploadFilter() != null) {
                    existingProject.setUploadFilter(project.getUploadFilter());
                }

                return existingProject;
            })
            .map(projectRepository::save);
    }

    /**
     * Get all the projects.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Project> findAll(Pageable pageable) {
        log.debug("Request to get all Projects");
        return projectRepository.findAll(pageable);
    }

    /**
     * Get one project by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Project> findOne(Long id) {
        log.debug("Request to get Project : {}", id);
        return projectRepository.findById(id);
    }

    /**
     * Delete the project by id with libraries.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Project : {}", id);
        dependencyService.deleteByProject(id);
        projectRepository.deleteById(id);
    }

    /**
     * Count all projects.
     * @return the number of projects.
     */
    public long count() {
        log.debug("Request to count all Projects");
        return projectRepository.count();
    }

    /**
     * Überprüfe, ob ein Projekt anhand der ID existiert.
     *
     * @param id ID des Projekts.
     * @return true, wenn das Projekt existiert, ansonsten false.
     */
    public boolean existsById(Long id) {
        log.debug("Request to check if a Project exist by ID : {}", id);
        return projectRepository.existsById(id);
    }

    /**
     * Get one project by identifier and version.
     *
     * @param identifier the project identifier
     * @param version    the project version
     * @return the optional project entity
     */
    public Optional<Project> findIdentifierVersion(String identifier, String version) {
        log.debug("Request to get Project : {} with Version : {}", identifier, version);
        return projectRepository.findByIdentifierAndVersion(identifier, version);
    }

    /**
     * Create a license ZIP file for a specific project.
     *
     * @param project the project entity
     * @return the license ZIP as a byte array
     */
    public byte[] createLicenseZip(Project project) throws FileNotFoundException {
        List<Library> libraries = dependencyService
            .findDependenciesByProjectId(project.getId())
            .stream()
            .filter(Dependency::isHidden)
            .map(Dependency::getLibrary)
            .collect(Collectors.toList());
        try {
            LicenseZipHelper licenseZipHelper = new LicenseZipHelper(project, libraries);
            licenseZipHelper.addLicenseTexts();
            licenseZipHelper.addCopyrights();
            licenseZipHelper.addDisclaimer(project.getDisclaimer());
            licenseZipHelper.addHtmlOverview("FOSS_Software_" + project.getName(), "style");

            return licenseZipHelper.getLicenseZip();
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    /**
     * Creates an OSS list as a CSV or HTML file.
     * It can create three different types of OSS lists.
     * DEFAULT: OSS list with all libraries and different information like license risk.
     * PUBLISH: OSS list with distinct libraries (based on GroupId, ArtifactId) and license(s) to be published.
     * REQUIREMENT: OSS list with all libraries and the corresponding requirements.
     *
     * @param project A project entity
     * @param format  {@link ExportFormat Export format}. Supports CSV or HTML
     * @param ossType The {@link OssType OSS list type}
     * @return A {@link File} object
     */
    public File createOssList(Project project, ExportFormat format, OssType ossType) throws ExportException {
        String fileName =
            project.getLabel() +
                "_" +
                project.getVersion() +
                "_" +
                "%{type}%" +
                "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT));

        byte[] content = new byte[]{};
        String contentType = null;
        Deque<String> headers = null;
        StringBuilder csvBuilder = null;
        List<Deque<String>> libraries = null;

        switch (ossType) {
            case REQUIREMENT:
                fileName = fileName.replace("%{type}%", Constants.OSS_REQUIREMENT_PREFIX);
                if (format.equals(ExportFormat.HTML)) {
                    try {
                        OssListHelper ossListHelper = new OssListHelper(
                            project,
                            dependencyService.findLibrariesByProjectId(project.getId()),
                            false
                        );

                        List<String> requirementsLookup = requirementRepository
                            .findAll()
                            .stream()
                            .map(Requirement::getShortText)
                            .collect(Collectors.toList());

                        ossListHelper.createFullHtml(requirementsLookup);
                        content = ossListHelper.getHtml().getBytes(StandardCharsets.UTF_8);
                    } catch (FileNotFoundException e) {
                        throw new ExportException("HTML template could not be found");
                    }
                } else if (format.equals(ExportFormat.CSV)) {
                    headers = new ArrayDeque<>(
                        Arrays.asList("GroupId", "ArtifactId", "Version", "License", "LicenseRisk", "LicensesTotal", "Comment", "ComplianceComment")
                    );

                    List<String> requirementsLookup = requirementRepository
                        .findAll()
                        .stream()
                        .map(Requirement::getShortText)
                        .collect(Collectors.toList());

                    headers.addAll(requirementsLookup);
                    csvBuilder = new StringBuilder(65536);
                    libraries = createOssListWithRequirements(project, requirementsLookup);
                }
                break;
            case DEFAULT:
                fileName = fileName.replace("%{type}%", Constants.OSS_DEFAULT_PREFIX);
                if (format.equals(ExportFormat.HTML)) {
                    try {
                        OssListHelper ossListHelper = new OssListHelper(
                            project,
                            dependencyService.findLibrariesByProjectId(project.getId()),
                            false
                        );
                        ossListHelper.createHml(false);
                        content = ossListHelper.getHtml().getBytes(StandardCharsets.UTF_8);
                    } catch (FileNotFoundException e) {
                        throw new ExportException("HTML template could not be found");
                    }
                } else if (format.equals(ExportFormat.CSV)) {
                    headers = new ArrayDeque<>(8);
                    headers.add("GroupId");
                    headers.add("ArtifactId");
                    headers.add("Version");
                    headers.add("Type");
                    headers.add("License");
                    headers.add("LicenseRisk");
                    headers.add("LicenseUrl");
                    headers.add("SourceCodeUrl");

                    csvBuilder = new StringBuilder(65536);

                    libraries =
                        dependencyService
                            .findLibrariesByProjectId(project.getId())
                            .stream()
                            .map(library -> {
                                Deque<String> records = new ArrayDeque<>(8);
                                records.add(library.getGroupId());
                                records.add(library.getArtifactId());
                                records.add(library.getVersion() != null ? "V" + library.getVersion() : "");
                                records.add(library.getType() != null ? library.getType().getValue() : "");
                                records.add(library.printLinkedLicenses());
                                records.add(library.getLicenseRisk(library.getLicenseToPublishes()).getName());
                                records.add(
                                    library.getLicenseUrl() != null &&
                                        !StringUtils.isBlank(library.getLicenseUrl()) &&
                                        !library.getLicenseUrl().equalsIgnoreCase(Constants.NO_URL)
                                        ? library.getLicenseUrl()
                                        : library.getLicenseToPublishes().stream().map(License::getUrl).collect(Collectors.joining(", "))
                                );
                                records.add(library.getSourceCodeUrl() != null ? library.getSourceCodeUrl() : "");
                                return records;
                            })
                            .collect(Collectors.toList());
                }
                break;
            case PUBLISH:
                fileName = fileName.replace("%{type}%", Constants.OSS_PUBLISH_PREFIX);
                if (format.equals(ExportFormat.HTML)) {
                    try {
                        OssListHelper ossListHelper = new OssListHelper(
                            project,
                            dependencyService
                                .findDependenciesByProjectId(project.getId())
                                .stream()
                                .filter(Dependency::isHidden)
                                .map(Dependency::getLibrary)
                                .collect(Collectors.toList()),
                            true
                        );
                        ossListHelper.createHml(true);
                        content = ossListHelper.getHtml().getBytes(StandardCharsets.UTF_8);
                    } catch (FileNotFoundException e) {
                        throw new ExportException("HTML template could not be found");
                    }
                } else if (format.equals(ExportFormat.CSV)) {
                    List<Library> libraryList = dependencyService
                        .findDependenciesByProjectId(project.getId())
                        .stream()
                        .filter(Dependency::isHidden)
                        .map(Dependency::getLibrary)
                        .collect(Collectors.toList());

                    OssListHelper ossListHelper = new OssListHelper(libraryList);
                    try {
                        byte[] csv = ossListHelper.createPublishCsv();

                        return new File(fileName + ".csv", csv, "text/csv");
                    } catch (IOException e) {
                        log.error("OSS list could not be created as a CSV : {}", e.getMessage());
                        throw new ExportException("Error while exporting OSS list");
                    }
                } else {
                    throw new ExportException("Unsupported OSS list format");
                }
                break;
            default:
                throw new ExportException("Unsupported OSS list type");
        }

        if (format.equals(ExportFormat.HTML)) {
            fileName = fileName + ".html";
            contentType = "text/html";
        } else if (format.equals(ExportFormat.CSV)) {
            fileName = fileName + ".csv";
            contentType = "text/csv";
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader(headers.toArray(new String[0]))
                .build();

            try (CSVPrinter csvPrinter = new CSVPrinter(csvBuilder, csvFormat)) {
                for (Deque<String> library : libraries) {
                    csvPrinter.printRecord(library.toArray());
                }

                csvPrinter.flush();
                content = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("OSS list could not be created as a CSV : {}", e.getMessage());
                throw new ExportException("Error while exporting OSS list");
            }
        }

        return new File(fileName, content, contentType);
    }

    /**
     * Helper for the "Full" OSS CSV report to create the content with library information and
     * the requirements to fulfill.
     *
     * @param project            For which the list should be created
     * @param requirementsLookup List with all requirements
     * @return List with Deque objects which contains all information per row in the CSV
     */
    private List<Deque<String>> createOssListWithRequirements(Project project, List<String> requirementsLookup) {
        List<Deque<String>> content = new ArrayList<>();
        Set<String> totalLicenses = new HashSet<>();

        List<Library> libraries = dependencyService.findLibrariesByProjectId(project.getId());
        for (Library dependency : libraries) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();
            String licenseRisk = dependency.getLicenseRisk(dependency.getLicenseToPublishes()).getName();
            List<String> requirements = new ArrayList<>(16);
            String comment = dependency.getComment();
            String complianceComment = dependency.getComplianceComment();

            // Set initially empty string for every requirement
            for (String ignored : requirementsLookup) {
                requirements.add("");
            }

            dependency.getLicenses().forEach(e -> totalLicenses.add(e.getLicense().getShortIdentifier()));

            if (dependency.getLicenseToPublishes() != null && !dependency.getLicenseToPublishes().isEmpty()) {
                for (License licenseToPublish : dependency.getLicenseToPublishes()) {
                    if (licenseToPublish.getRequirements() != null) {
                        for (Requirement requirement : licenseToPublish.getRequirements()) {
                            if (requirementsLookup.contains(requirement.getShortText())) {
                                requirements.remove(requirementsLookup.indexOf(requirement.getShortText()));
                                requirements.add(requirementsLookup.indexOf(requirement.getShortText()), "X");
                            }
                        }
                    }
                }
            }

            List<String> libraryRow = new ArrayList<>(Arrays.asList(
                groupId,
                artifactId,
                "V" + version,
                dependency.printLinkedLicenses(),
                licenseRisk,
                String.valueOf(totalLicenses.size()),
                comment != null ? comment : "",
                complianceComment != null ? complianceComment : ""
            ));

            libraryRow.addAll(requirements);
            content.add(new ArrayDeque<>(libraryRow));
        }

        return content;
    }

    public ProjectOverview getProjectOverview(Project project) {
        long numberOfLibrariesPrevious;
        long numberOfLicensesPrevious;

        // If there is no previous project then set values to zero
        if (project.getPreviousProject() == null) {
            numberOfLibrariesPrevious = 0;
            numberOfLicensesPrevious = 0;
        } else {
            // Total libraries from previous version
            numberOfLibrariesPrevious = dependencyService.countLibrariesByProject(project.getPreviousProject().getId());

            // Number of licenses from the previous project version
            numberOfLicensesPrevious = dependencyService
                .countDistributedLicensesByProjectId(project.getPreviousProject().getId())
                .stream()
                .map(CountOccurrences::getValue)
                .count();
        }

        // Total libraries from current project version
        long numberOfLibraries = dependencyService.countLibrariesByProject(project.getId());

        // Number of licenses from current project version
        long numberOfLicenses = dependencyService
            .countDistributedLicensesByProjectId(project.getId())
            .stream()
            .map(CountOccurrences::getValue)
            .count();

        // Total number of reviewed libraries for the current project
        long numberOfReviewedLibraries = dependencyService.countReviewedLibrariesByProject(project.getId());

        return new ProjectOverview(
            numberOfLibraries,
            numberOfLibrariesPrevious,
            numberOfLicenses,
            numberOfLicensesPrevious,
            numberOfReviewedLibraries
        );
    }

    /**
     * Get a {@link ProjectOverview} of a project.
     *
     * @param project project entity
     * @return a ProjectOverview
     */
    public ProjectStatistic getProjectStatistic(Project project) {
        // Distributed licenses list
        List<CountOccurrences> distributedLicenses = dependencyService.countDistributedLicensesByProjectId(project.getId());

        List<Series> licenseRiskSeries = new ArrayList<>(7);
        List<Series> numberOfLibrariesSeries = new ArrayList<>(1);

        // Add initial total libraries from current project
        Series librariesSeries = new Series("Libraries", new ArrayDeque<>(Constants.MAX_SERIES_LIMIT));
        librariesSeries
            .getSeries()
            .addFirst(new CountOccurrences(project.getVersion(), dependencyService.countLibrariesByProject(project.getId())));
        numberOfLibrariesSeries.add(librariesSeries);

        // Add initial risks from current project
        getProjectRisk(project)
            .forEach(e -> {
                Deque<CountOccurrences> riskPerVersion = new ArrayDeque<>(Constants.MAX_SERIES_LIMIT);
                riskPerVersion.add(new CountOccurrences(project.getVersion(), e.getValue()));
                licenseRiskSeries.add(new Series(e.getName(), riskPerVersion));
            });

        // Get previous project
        Optional<Project> prevProject = Optional.empty();
        if (project.getPreviousProject() != null) {
            prevProject = projectRepository.findById(project.getPreviousProject().getId());
        }

        int maxSeriesCounter = 1;
        while (prevProject.isPresent() && maxSeriesCounter <= Constants.MAX_SERIES_LIMIT) {
            List<CountOccurrences> risks = getProjectRisk(prevProject.get());
            librariesSeries.addSeriesEntry(
                prevProject.get().getVersion(),
                dependencyService.countLibrariesByProject(prevProject.get().getId())
            );

            int counter = 0;
            for (CountOccurrences e : risks) {
                Series risk = licenseRiskSeries.get(counter);

                if (e.getName().equals(risk.getName())) {
                    risk.addSeriesEntry(prevProject.get().getVersion(), e.getValue());
                }

                counter++;
            }

            if (prevProject.get().getPreviousProject() != null) {
                prevProject = projectRepository.findById(prevProject.get().getPreviousProject().getId());
            } else {
                prevProject = Optional.empty();
            }
            maxSeriesCounter++;
        }

        return new ProjectStatistic(distributedLicenses, getProjectRisk(project), licenseRiskSeries, numberOfLibrariesSeries);
    }

    /**
     * Count the number of libraries by the license risks of a project.
     *
     * @param project Project entity
     * @return an ordered map of license risk name and number of libraries.
     */
    public List<CountOccurrences> getProjectRisk(Project project) {
        List<Library> libraries = dependencyService.findLibrariesByProjectId(project.getId());

        return getProjectRisk(libraries);
    }

    /**
     * Count the number of libraries by the license risks of a project.
     *
     * @param id Project id
     * @return an ordered map of license risk name and number of libraries.
     */
    public List<CountOccurrences> getProjectRisk(Long id) {
        List<Library> libraries = dependencyService.findLibrariesByProjectId(id);

        return getProjectRisk(libraries);
    }

    /**
     * Count the number of libraries by the license risks of a project.
     *
     * @param libraries Libraries of a Project
     * @return an ordered map of license risk name and number of libraries.
     */
    public List<CountOccurrences> getProjectRisk(List<Library> libraries) {
        List<CountOccurrences> risks = new ArrayList<>(8);

        licenseRiskRepository
            .findAll()
            .forEach(risk -> {
                long riskCount = libraries
                    .stream()
                    .filter(library -> library.getLicenseRisk(library.getLicenseToPublishes()).getId().equals(risk.getId()))
                    .count();
                risks.add(new CountOccurrences(risk.getName(), riskCount));
            });

        return risks;
    }

    /**
     * Count the number of licenses of a project.
     *
     * @param project project entity
     * @return a map of license names (shortIdentifier) and number of licenses.
     */
    public List<CountOccurrences> getProjectLicenses(Project project) {
        return dependencyService.countDistributedLicensesByProjectId(project.getId());
    }

    /**
     * Create an archive from the libraries in a project. Include all or only the necessary libraries to the archive.
     * This archive can be downloaded or directly transferred to target that is specified in the application config.
     *
     * @param project  project for which the archive should be created
     * @param format   an {@link ArchiveFormat} (FULL or DELIVERY)
     * @param shipment the {@link ShipmentMethod} of the archive (DOWNLOAD or EXPORT)
     * @return Map of the archive file and status (archive in/complete)
     * @throws StorageException          if a problem by writing to the local system occurs
     * @throws RemoteRepositoryException if the remote repository is not accessible
     * @throws ZIPException              if the ZIP archive cannot be created
     * @throws UploadException           if the ZIP archive could not be made downloadable or the upload to the remote repository fails
     * @throws FileNotFoundException     if the local storage folder cannot be found
     */
    public Map<File, Boolean> createArchive(Project project, ArchiveFormat format, ShipmentMethod shipment)
        throws StorageException, RemoteRepositoryException, ZIPException, UploadException, FileNotFoundException {
        //Starts the creation of the archive
        archiveLog.info("Creating new archive for {} - {}.", project.getName(), project.getVersion());

        //Initializes the Map to be returned
        //complete indicates if the source code was found for all libraries
        var complete = true;
        Map<File, Boolean> returnMap = new HashMap<>(1);

        //Loads the settings
        //application.properties are ultimately retrieved from the docker-compose file
        String remoteIndexFilePath = applicationProperties.getSourceCodeArchive().getRemoteIndex();
        String[] remoteIndexPaths = remoteIndexFilePath.split("/");
        String remoteIndexFileName = remoteIndexPaths[(remoteIndexPaths.length) - 1];
        String remoteIndexPath = remoteIndexFilePath.replaceAll(remoteIndexFileName + "$", "");
        String remoteArchivePath = applicationProperties.getSourceCodeArchive().getRemoteArchive();
        String uploadPlatformURL = applicationProperties.getSourceCodeArchive().getUploadPlatform();
        String uploadUser = applicationProperties.getSourceCodeArchive().getUploadUser();
        String uploadPassword = applicationProperties.getSourceCodeArchive().getUploadPassword();

        //If no platform for the export is given, but export is requested, an UploadException is thrown
        if (shipment.equals(ShipmentMethod.EXPORT) && StringUtils.isBlank(uploadPlatformURL)) {
            archiveLog.error("External platform for upload not defined.");
            throw new UploadException("Platform for upload not defined.");
        }

        //Parses the archive's name based on the requested format
        String archiveName;
        if (format.equals(ArchiveFormat.FULL)) {
            archiveName = project.getLabel() + "_" + project.getVersion() + "_full-3rd-party-library-archive";
        } else {
            archiveName = project.getLabel() + "_" + project.getVersion() + "_3rd-party-library-archive-for-delivery";
        }

        //Sets the local paths and files
        var localStorageDirectory = ResourceUtils.getFile(SourceCodeHelper.LOCAL_STORAGE_URI);
        var localStoragePath = localStorageDirectory.getPath() + Constants.FILE_SEPARATOR;
        var localArchiveDirectory = new java.io.File(localStoragePath + archiveName);
        var localArchivePath = localArchiveDirectory.getPath() + Constants.FILE_SEPARATOR;
        var localIndexFile = new java.io.File(localStoragePath + Constants.INDEX);

        //Prepares the local file system, throws a StorageException if an error occurs
        //(Re)Creates the local archive directory
        try {
            FileUtils.deleteDirectory(localArchiveDirectory);
            FileUtils.forceMkdir(localArchiveDirectory);
        } catch (IOException e) {
            archiveLog.error("Could not create new archive {} in local storage {} : {}", archiveName, localStoragePath, e.getMessage());
            throw new StorageException("Could not access local filesystem.");
        }
        //Deletes local, outdated index.csv file if existing
        try {
            FileUtils.forceDelete(localIndexFile);
        } catch (FileNotFoundException e) {
            archiveLog.info("Index.csv is not existing in local storage {}.", localStoragePath);
        } catch (IOException e) {
            archiveLog.error("Could not delete index.csv in local storage {} : {}", localStoragePath, e.getMessage());
            throw new StorageException("Could not access local filesystem.");
        }

        //Downloads the up-to-date index.csv file of the remote 3rd-party repository, throws a RemoteRepositoryException if the file is not available
        try {
            HttpHelper.downloadResource(remoteIndexFilePath, localIndexFile);
        } catch (IOException e) {
            archiveLog.error("Could not download index.csv file from {} : {}", remoteIndexFilePath, e.getMessage());
            throw new RemoteRepositoryException("Could not access the remote repository.");
        }

        // Retrieve all libraries for the specified projectId without hidden libraries
        List<Library> libraries = dependencyService
            .findDependenciesByProjectId(project.getId())
            .stream()
            .filter(Dependency::isHidden)
            .map(Dependency::getLibrary)
            .collect(Collectors.toList());

        //Iterates through all the libraries of the project
        for (Library library : libraries) {
            //Full identifier of the library in 3rd-party repository (with file extension)
            String identifier;
            //Short identifier of the library in the internal 3rd-party repository (without file extension)
            String label = SourceCodeHelper.createLabel(library);

            //For the delivery archive, checks if the library must be included
            //If not, skips the library
            if (format.equals(ArchiveFormat.DELIVERY) && !SourceCodeHelper.isCodeSharingRequired(library)) {
                archiveLog.info("Library {} does not need to be contained in delivery archive.", label);
                continue;
            }

            //Parses the full identifier from the index.csv file via the label
            try {
                identifier = SourceCodeHelper.checkRepository(localIndexFile, label);

                if (identifier == null) {
                    if (library.sourceCodeUrlIsValid()) {
                        archiveLog.info(
                            "Library {} is not available in the 3rd-party repository. Trying to download the source code URL from the library.",
                            label
                        );
                        SourceCodeHelper.updateRepository(label, library, localIndexFile, localArchivePath, remoteArchivePath);
                    } else {
                        identifier = SourceCodeHelper.checkRepositoryWithFuzzySearch(localIndexFile, label);

                        if (identifier != null) {
                            archiveLog.info("The source code archive was found using the fuzzy search : {}", identifier);
                            library.addErrorLog(
                                "Source Code",
                                "The source code archive was found using the fuzzy search." +
                                    " This may not be the right archive and should be verified : " +
                                    identifier,
                                LogSeverity.MEDIUM
                            );

                            var libraryPackage = new java.io.File(localArchivePath + identifier);
                            HttpHelper.downloadResource(remoteArchivePath + identifier, libraryPackage);
                        }
                    }
                } else {
                    archiveLog.info("Library {} is available in the 3rd-party repository and will be downloaded from there.", label);
                    var libraryPackage = new java.io.File(localArchivePath + identifier);
                    HttpHelper.downloadResource(remoteArchivePath + identifier, libraryPackage);
                }
            } catch (IOException e) {
                complete = false;
                archiveLog.error("Could not download library {} : {}", label, e.getMessage());
            }
        }

        //Uploads the local, updated index.csv file to the remote repository, throws a RemoteRepositoryException if the upload fails
        //remoteIndexPath
        //remoteIndexFile
        try {
            HttpHelper.transferToTarget(remoteIndexPath, Files.readAllBytes(localIndexFile.toPath()), remoteIndexFileName);
        } catch (IOException e) {
            archiveLog.error("Could not upload index.csv file to {} : {}", remoteIndexFilePath, e.getMessage());
            throw new RemoteRepositoryException("Could not access the remote repository.");
        }

        //Zips the archive folder, throws a ZIPException if the zipping fails
        java.io.File zipFile;
        try {
            zipFile = SourceCodeHelper.createZip(localArchiveDirectory);
        } catch (IOException e) {
            archiveLog.error("Could not create zip file {}.zip in directory {} : {}", archiveName, localStoragePath, e.getMessage());
            throw new ZIPException("Could not create the archive.");
        }

        //TODO: The option to download in-browser might not work for big files, activate and rework this
        //long threshold = Runtime.getRuntime().maxMemory() - 1*1000000000;
        //if (zipFile.length() > threshold) {
        //    log.debug("Threshold exceeded.");
        //}

        //Uploads the archive file to the remote repository or enables in-browser-download
        if (shipment.equals(ShipmentMethod.EXPORT)) {
            //Uploads the zip file, throws an UploadException if it fails
            //Adds an empty file to the Return-Map
            try {
                HttpHelper.transferZipToTargetWithCredentials(uploadPlatformURL, uploadUser, uploadPassword, zipFile);
                returnMap.put(new File(archiveName + ".zip", new byte[0], "N/A"), complete);
            } catch (IOException e) {
                archiveLog.error("Could not upload zip file {}.zip to platform {} : {}", archiveName, uploadPlatformURL, e.getMessage());
                throw new UploadException("Could not access the upload platform.");
            }
        } else {
            //Adds the file to the Return-Map, throws an UploadException if it fails
            byte[] content;
            try {
                content = Files.readAllBytes(zipFile.toPath());
                returnMap.put(new File(archiveName + ".zip", content, "application/zip"), complete);
            } catch (IOException e) {
                archiveLog.error("Could not make zip file {}.zip downloadable: {}", archiveName, e.getMessage());
                throw new UploadException("Could not prepare the archive for download.");
            }
        }

        //Returns the Map
        archiveLog.info("Archive successfully created.");
        return returnMap;
    }

    public ProjectOverview createProjectOverview() {
        return new ProjectOverview();
    }

    /**
     * Create the project version based on another project. Copies all fields except the old version and the libraries
     * list but the manually added libraries can be copied.
     *
     * @param project        Project entity
     * @param version   Version of the new project
     * @param delivered Set the delivered filed of the base project to true or not
     * @param copying   Copy manually added libraries to the new project or not
     * @return The new project
     */
    public Project createNextVersion(Project project, String version, boolean delivered, boolean copying) {
        // Detach base project and change attributes
        //Project project = entityManager.find(Project.class, id);
        project.setDelivered(delivered);
        project.setDeliveredDate(Instant.now());
        Project projectNew = entityManager.merge(project);

        entityManager.flush();
        entityManager.detach(projectNew);

        projectNew.setId(null);
        projectNew.setVersion(version);
        projectNew.setPreviousProject(project);
        projectNew.setDelivered(false);
        projectNew.setDeliveredDate(null);
        projectNew.setLastUpdatedDate(null);
        projectNew.setCreatedDate(LocalDate.now());

        projectNew = entityManager.merge(projectNew);

        // Copy manually added libraries to new project
        if (copying) {
            Project lppProject = projectNew;
            dependencyService
                .findDependenciesByProjectIdAndAddedManuallyIsTrue(project.getId())
                .forEach(dependency -> {
                    Dependency lpp = new Dependency()
                        .project(lppProject)
                        .library(dependency.getLibrary())
                        .addedManually(dependency.getAddedManually());
                    try {
                        dependencyService.saveWithCheck(lpp);
                    } catch (LibraryException e) {
                        log.debug("Library [ {} ] is already in Project [ {} ]", lpp.getLibrary().getId(), lpp.getProject().getId());
                    }
                });
        }

        return projectNew;
    }

    /**
     * Processing of an SBOM for a project. Based on the SBOM format the AssetManager recognizes which loader has to be
     * selected. The loaders are registered individually in the constructor. If the SBOM contains duplicates, then these
     * are added only once. Likewise, the components, which are already contained in the project (delete=false) are
     * checked, so that no renewed adding takes place.
     *
     * @param project Project entity.
     * @param upload  {@link Upload} object with SBOM.
     * @param delete  True if already existing components should be deleted from the project, otherwise components from
     *                the upload will be added to the previous list.
     * @throws UploadException If the project cannot be found or errors occur when processing the Upload.
     */
    @Async
    @Transactional
    public void processUpload(Project project, Upload upload, boolean delete) throws UploadException {
        log.info("Start processing upload for Project : {}", project.getId());

        try {
            Set<Library> libraries = assetManager.load(upload.getFile(), upload.getFile().getFileContentType());

            if (libraries != null) {
                if (delete) {
                    dependencyService.deleteByProjectAndNotAddedManually(project.getId());
                }

                Stream<Library> libraryStream = libraries.stream();

                if (!StringUtils.isBlank(project.getUploadFilter())) {
                    for (String line : project.getUploadFilter().split("\n")) {
                        if (!StringUtils.isBlank(line)) {
                            libraryStream =
                                libraryStream.filter(library ->
                                    !(Pattern.matches(line, library.getGroupId()) || Pattern.matches(line, library.getArtifactId()))
                                );
                        }
                    }
                }

                AtomicInteger libraryCounter = new AtomicInteger(0);

                libraryStream.forEach(library -> {
                    log.info(
                        "[{}] Processing library for project {} : {} - {} - {}",
                        libraryCounter.get(),
                        project.getId(),
                        library.getGroupId(),
                        library.getArtifactId(),
                        library.getVersion()
                    );
                    libraryCounter.incrementAndGet();

                    try {
                        if (library.getLastReviewedBy() != null) {
                            Optional<User> optionalUser = userRepository.findOneByLogin(library.getLastReviewedBy().getLogin());
                            optionalUser.ifPresent(library::setLastReviewedBy);
                        }

                        library = libraryService.saveWithCheck(library);
                    } catch (LibraryException e) {
                        Library dbLibrary = e.getLibrary();
                        dbLibrary.updateEmptyFields(library);

                        library = new Pipeline<>(new MavenLicenseStep()).pipe(new NpmLicenseStep()).execute(library);
                        libraryService.licenseAutocomplete(dbLibrary);
                        //libraryService.hasIncompatibleLicenses(library);
                        libraryService.removeGenericLicenseUrl(dbLibrary);
                        libraryService.urlAutocomplete(dbLibrary);
                        libraryService.licenseTextAutocomplete(library);
                        libraryService.copyrightAutocomplete(dbLibrary);
                        libraryService.calculateLibraryRisk(library);
                        library = libraryService.save(dbLibrary);
                    }

                    Dependency dependency = new Dependency();
                    dependency.setProject(project);
                    dependency.setLibrary(library);

                    try {
                        dependencyService.saveWithCheck(dependency);
                    } catch (LibraryException e) {
                        log.debug("Library [ {} ] is already in Project [ {} ]", library.getId(), project.getId());
                    }
                });

                if (upload.getAdditionalLibraries() != null && upload.getAdditionalLibraries().getFile() != null) {
                    AdditionalLibrariesLoader additionalLibrariesLoader = new AdditionalLibrariesLoader();
                    Set<Library> additionalLibraries = additionalLibrariesLoader.load(upload.getAdditionalLibraries());

                    for (Library library : additionalLibraries) {
                        log.info(
                            "Processing additional library : {} - {} - {}",
                            library.getGroupId(),
                            library.getArtifactId(),
                            library.getVersion()
                        );

                        try {
                            library = libraryService.saveWithCheck(library);
                        } catch (LibraryException e) {
                            library = e.getLibrary();
                        }

                        Dependency dependency = new Dependency();
                        dependency.setProject(project);
                        dependency.setLibrary(library);
                        dependency.setAddedManually(true);

                        try {
                            dependencyService.saveWithCheck(dependency);
                        } catch (LibraryException e) {
                            log.debug("Library [ {} ] is already in Project [ {} ]", library.getId(), project.getId());
                        }
                    }
                }

                project.setUploadState(UploadState.OK);
                project.setLastUpdatedDate(LocalDate.now());
                saveWithCheck(project);
            }
            log.info("Finished processing upload!");
        } catch (Exception e) { // Catch alle Exceptions
            project.setUploadState(UploadState.FAILURE);
            save(project);

            throw new UploadException(e.getMessage());
        }
    }

    /**
     * Check if an upload by URL is valid.
     *
     * @param project Project entity
     * @param url     URL to a file
     * @return Content type of the file
     * @throws UploadException if the file doesn't exist, can't be downloaded or exceeds the upload limit size.
     */
    public String preCheckForUploadByUrl(Project project, String url, BasicAuthentication credentials) throws UploadException {
        log.info("Start processing upload by URL for Project : {}", project.getId());

        int uploadLimit = applicationProperties.getUpload().getLimit();

        String contentType;
        try {
            HttpResponse<String> response;
            if (StringUtils.isBlank(credentials.getUsername()) || StringUtils.isBlank(credentials.getPassword())) {
                response = HttpHelper.httpHeadRequest(url);
            } else {
                response = HttpHelper.httpHeadRequestWithAuthorization(url, credentials.getUsername(), credentials.getPassword());
            }

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                log.error("Response code is not 200. Wrong url, credentials or another access problem.");
                throw new UploadException("File cannot be accessed.");
            }

            Optional<String> optionalContentLength = response.headers().firstValue("Content-Length");
            if (optionalContentLength.isPresent()) {
                String contentLengthResponse = optionalContentLength.get();
                long contentLength = Long.parseLong(contentLengthResponse);
                contentLength = contentLength / 1000 / 1000;

                if (contentLength > uploadLimit) {
                    log.error("File size is too large. Maximum upload limit: {} MB", uploadLimit);
                    throw new UploadException("File size is too large. Maximum upload limit: " + uploadLimit + " MB");
                }
            }

            Optional<String> optionalContentType = response.headers().firstValue("Content-Type");
            if (optionalContentType.isPresent()) {
                contentType = optionalContentType.get();
            } else {
                throw new UploadException(
                    "Undefined content type. For the subsequent processing the 'Content-Type' HTTP header is necessary."
                );
            }
        } catch (NumberFormatException e) {
            throw new UploadException("Incompatible upload");
        } catch (URISyntaxException | IOException | InterruptedException e) {
            project.setUploadState(UploadState.FAILURE);
            save(project);
            throw new UploadException("Cannot download file : " + e.getMessage());
        }

        return contentType;
    }

    @Async
    public void processUploadByUrl(Project project, String url, BasicAuthentication credentials, boolean delete, String contentType)
        throws UploadException {
        log.info("Start processing upload by URL for Project : {}", project.getId());
        try (
            InputStream downloadStream = (
                StringUtils.isBlank(credentials.getUsername()) || StringUtils.isBlank(credentials.getPassword())
                    ? HttpHelper.downloadResource(url)
                    : HttpHelper.downloadResourceWithAuthentication(url, credentials.getUsername(), credentials.getPassword())
            )
        ) {
            File file = new File("Upload", downloadStream, contentType);
            Upload upload = new Upload();
            upload.setFile(file);

            // Will not be asynchronously executed
            // Async method call in same class doesn't work
            processUpload(project, upload, delete);

            upload.getFile().getFilestream().close();
        } catch (IOException e) {
            project.setUploadState(UploadState.FAILURE);
            save(project);
            throw new UploadException("Cannot download file : " + e.getMessage());
        } finally {
            System.gc();
        }
    }

    /**
     * Add {@link Library libraries} to a project. These libraries will be marked as "manually added" in the project.
     *
     * @param project   The project to add the library list
     * @param libraries A list of {@link Library libraries}
     */
    public void addLibraries(Project project, List<Library> libraries) {
        for (Library library : libraries) {
            Dependency dependency = new Dependency().project(project).library(library).addedManually(true);
            try {
                dependencyService.saveWithCheck(dependency);

                project.setLastUpdatedDate(LocalDate.now());
                save(project);
            } catch (LibraryException e) {
                log.debug("Library [ {} ] is already in Project [ {} ]", library.getId(), project.getId());
            }
        }
    }

    /**
     * Search in a project line the project that is in status "In Development".
     * The identifier is used from the passed project to execute the search. If multiple projects or none
     * are "In Development" then a {@link ProjectException} will be thrown.
     *
     * @param project The project with which to search for the project that is "In Development" from the project line
     * @return The project which is in status "In Development"
     * @throws ProjectException if no unique project could be found.
     */
    public Project getInDevelopmentProject(Project project) throws ProjectException {
        Optional<Project> optionalProject = projectRepository.findByIdAndDeliveredIsFalse(project.getLabel());

        if (optionalProject.isPresent()) {
            return optionalProject.get();
        } else {
            throw new ProjectException("No unique project which is \"In Development\" could be found");
        }
    }

    public DifferenceView compare(Project firstProject, Project secondProject) {
        List<Library> firstProjectWithoutDuplicates = dependencyService.onlyLibrariesFromFirstProjectWithoutIntersection(
            firstProject.getId(),
            secondProject.getId()
        );

        List<Library> secondProjectWithoutDuplicates = dependencyService.onlyLibrariesFromFirstProjectWithoutIntersection(
            secondProject.getId(),
            firstProject.getId()
        );

        List<Library> firstProjectLibraries = dependencyService.findLibrariesByProjectId(firstProject.getId());
        List<Library> secondProjectLibraries = dependencyService.findLibrariesByProjectId(secondProject.getId());

        List<Library> firstProjectNewLibraries = new ArrayList<>(16);

        for (Library libraryFromFirstProjectWithoutDuplicates : firstProjectWithoutDuplicates) {
            boolean notUnique = false;

            for (Library libraryFromSecondProject : secondProjectLibraries) {
                if (DifferenceView.isEqualLibraryNameAndType(libraryFromFirstProjectWithoutDuplicates, libraryFromSecondProject)) {
                    notUnique = true;
                }
            }

            if (!notUnique) {
                firstProjectNewLibraries.add(libraryFromFirstProjectWithoutDuplicates);
            }
        }

        List<Library> secondProjectNewLibraries = new ArrayList<>(16);

        for (Library libraryFromSecondProjectWithoutDuplicates : secondProjectWithoutDuplicates) {
            boolean notUnique = false;

            for (Library libraryFromFirstProject : firstProjectLibraries) {
                if (DifferenceView.isEqualLibraryNameAndType(libraryFromSecondProjectWithoutDuplicates, libraryFromFirstProject)) {
                    notUnique = true;
                }
            }

            if (!notUnique) {
                secondProjectNewLibraries.add(libraryFromSecondProjectWithoutDuplicates);
            }
        }

        return new DifferenceView(
            dependencyService.libraryIntersectionOfProjects(firstProject.getId(), secondProject.getId()),
            firstProjectWithoutDuplicates,
            secondProjectWithoutDuplicates,
            firstProjectNewLibraries,
            secondProjectNewLibraries
        );
    }
}
