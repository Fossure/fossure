package io.github.fossure.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fossure.domain.*;
import io.github.fossure.repository.LicenseRiskRepository;
import io.github.fossure.repository.RequirementRepository;
import io.github.fossure.repository.UploadRepository;
import io.github.fossure.repository.UserRepository;
import io.github.fossure.service.exceptions.LibraryException;
import io.github.fossure.service.exceptions.LicenseAlreadyExistException;
import io.github.fossure.service.exceptions.LicenseException;
import io.github.fossure.service.exceptions.UploadException;
import io.github.fossure.service.upload.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Service implementation for managing {@link Upload}.
 */
@Service
@Transactional
public class UploadService {

    private final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final UploadRepository uploadRepository;

    private final LibraryService libraryService;

    private final LicenseService licenseService;

    private final UserRepository userRepository;

    private final LicenseRiskRepository licenseRiskRepository;

    private final RequirementRepository requirementRepository;

    private final ObjectMapper objectMapper;

    public UploadService(
        UploadRepository uploadRepository,
        LibraryService libraryService,
        LicenseService licenseService,
        UserRepository userRepository,
        LicenseRiskRepository licenseRiskRepository,
        RequirementRepository requirementRepository,
        ObjectMapper objectMapper
    ) {
        this.uploadRepository = uploadRepository;
        this.libraryService = libraryService;
        this.licenseService = licenseService;
        this.userRepository = userRepository;
        this.licenseRiskRepository = licenseRiskRepository;
        this.requirementRepository = requirementRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Save a upload.
     *
     * @param upload the entity to save.
     * @return the persisted entity.
     */
    public Upload save(Upload upload) {
        log.debug("Request to save Upload : {}", upload);

        if (upload.getFileContentType().equals("application/vnd.ms-excel")) upload.setFileContentType("text/csv");

        return uploadRepository.save(upload);
    }

    /**
     * Partially update a upload.
     *
     * @param upload the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Upload> partialUpdate(Upload upload) {
        log.debug("Request to partially update Upload : {}", upload);

        return uploadRepository
            .findById(upload.getId())
            .map(existingUpload -> {
                if (upload.getFile() != null) {
                    existingUpload.setFile(upload.getFile());
                }
                if (upload.getFileContentType() != null) {
                    existingUpload.setFileContentType(upload.getFileContentType());
                }
                if (upload.getEntityToUpload() != null) {
                    existingUpload.setEntityToUpload(upload.getEntityToUpload());
                }
                if (upload.getRecord() != null) {
                    existingUpload.setRecord(upload.getRecord());
                }
                if (upload.getOverwriteData() != null) {
                    existingUpload.setOverwriteData(upload.getOverwriteData());
                }
                if (upload.getUploadedDate() != null) {
                    existingUpload.setUploadedDate(upload.getUploadedDate());
                }

                return existingUpload;
            })
            .map(uploadRepository::save);
    }

    /**
     * Get all the uploads.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Upload> findAll(Pageable pageable) {
        log.debug("Request to get all Uploads");
        return uploadRepository.findAll(pageable);
    }

    /**
     * Get one upload by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Upload> findOne(Long id) {
        log.debug("Request to get Upload : {}", id);
        return uploadRepository.findById(id);
    }

    /**
     * Delete the upload by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Upload : {}", id);
        uploadRepository.deleteById(id);
    }

    /**
     * Handler to process uploaded file that contains libraries.
     * Can parse libraries from CSV and XML (CycloneDX BOM) files.
     * Saves and updates libraries to the {@link Library} or {@link Product} entity.
     *
     * @param upload the Upload entity to process.
     * @throws UploadException if an error occurs during processing of the uploaded file.
     */
    @SuppressWarnings("unchecked")
    @Async
    public void uploadHandler(Upload upload) throws UploadException {
        log.info("Start processing upload : {}", upload.getId());
        switch (upload.getEntityToUpload()) {
            case LIBRARY:
                AssetManager<Library> assetManager = new AssetManager<>();
                assetManager.addLoader(new BomLoader(), "text/xml");
                assetManager.addLoader(new LibraryCsvLoader(), "application/vnd.ms-excel");
                assetManager.addLoader(new LibraryCsvLoader(), "text/csv");
                assetManager.addLoader(new ArchiveLoader(libraryService), "application/zip");
                assetManager.addLoader(new ArchiveLoader(libraryService), "application/x-zip-compressed");
                assetManager.addLoader(
                    file -> {
                        try {
                            return objectMapper.readValue(file.getFile(), new TypeReference<>() {});
                        } catch (IOException e) {
                            log.error("Error while parsing JSON file : {}", e.getMessage());
                            throw new UploadException("JSON file can't be read");
                        }
                    },
                    "application/json"
                );
                Set<Library> libraries = assetManager.load(
                    new File("", upload.getFile(), upload.getFileContentType()),
                    upload.getFileContentType()
                );

                if (libraries != null) {
                    for (Library library : libraries) {
                        log.info("Processing library : {} - {} - {}", library.getGroupId(), library.getArtifactId(), library.getVersion());
                        try {
                            if (library.getLastReviewedBy() != null) {
                                Optional<User> optionalUser = userRepository.findOneByLogin(library.getLastReviewedBy().getLogin());
                                optionalUser.ifPresent(library::setLastReviewedBy);
                            }

                            // TODO: linkedLicenses field

                            /*Optional<String> licenses = library.getLicenses().stream().map(License::getFullName).findFirst();
                            if (licenses.isPresent()) {
                                library.setLicenses(licenseService.findShortIdentifier(licenses.get()));
                            }*/

                            Optional<String> licenseToPublish = library
                                .getLicenseToPublishes()
                                .stream()
                                .map(License::getFullName)
                                .findFirst();
                            if (licenseToPublish.isPresent()) {
                                library.setLicenseToPublishes(licenseService.findShortIdentifier(licenseToPublish.get()));
                            }

                            Optional<String> licenseOfFiles = library.getLicenseOfFiles().stream().map(License::getFullName).findFirst();
                            if (licenseOfFiles.isPresent()) {
                                library.setLicenseOfFiles(licenseService.findShortIdentifier(licenseOfFiles.get()));
                            }

                            library.setLicenseUrl("");
                            library.setSourceCodeUrl("");

                            library = libraryService.saveWithCheck(library);
                        } catch (LibraryException e) {
                            Library dbLibrary = e.getLibrary();
                            dbLibrary.updateEmptyFields(library);
                            libraryService.licenseAutocomplete(dbLibrary);
                            libraryService.removeGenericLicenseUrl(dbLibrary);
                            // library.setLicenseUrl("");
                            // library.setSourceCodeUrl("");
                            libraryService.urlAutocomplete(dbLibrary);
                            libraryService.copyrightAutocomplete(dbLibrary);
                            libraryService.save(dbLibrary);
                        }
                    }
                }
                break;
            case LICENSE:
                AssetManager<License> assetManagerLicense = new AssetManager<>();
                assetManagerLicense.addLoader(new LicenseCsvLoader(), "application/vnd.ms-excel");
                assetManagerLicense.addLoader(new LicenseCsvLoader(), "text/csv");
                assetManagerLicense.addLoader(
                    file -> {
                        try {
                            Set<License> licenses = objectMapper.readValue(file.getFile(), new TypeReference<>() {});
                            licenses.forEach(e -> e.setId(null));
                            licenses.forEach(e -> e.setLicenseConflicts(new TreeSet<>()));
                            licenses.forEach(e -> e.setLastReviewedBy(null));
                            return licenses;
                        } catch (IOException e) {
                            log.error("Error while parsing JSON file : {}", e.getMessage());
                            throw new UploadException("JSON file cannot be read");
                        }
                    },
                    "application/json"
                );

                Set<License> licenses = assetManagerLicense.load(
                    new File("", upload.getFile(), upload.getFileContentType()),
                    upload.getFileContentType()
                );

                for (License license : licenses) {
                    log.info("Processing license : {}", license.getShortIdentifier());

                    if (license.getLastReviewedBy() != null) {
                        Optional<User> optionalUser = userRepository.findOneByLogin(license.getLastReviewedBy().getLogin());
                        optionalUser.ifPresentOrElse(license::setLastReviewedBy, () -> license.setLastReviewedBy(null));
                        optionalUser.ifPresent(license::setLastReviewedBy);
                    }

                    if (license.getLicenseRisk() != null) {
                        Optional<LicenseRisk> risk = licenseRiskRepository.findOneByName(license.getLicenseRisk().getName());
                        risk.ifPresent(license::setLicenseRisk);
                    }

                    if (license.getRequirements().size() > 1) {
                        Set<Requirement> requirementEntities = license
                            .getRequirements()
                            .stream()
                            .map(e -> {
                                Optional<Requirement> optionalRequirement = requirementRepository.findOneByShortText(e.getShortText());
                                if (optionalRequirement.isPresent()) {
                                    return optionalRequirement.get();
                                } else {
                                    e.setId(null);
                                    return requirementRepository.save(e);
                                }
                            })
                            .collect(Collectors.toSet());

                        license.setRequirements(requirementEntities);
                    } else {
                        Optional<String> optionalRequirements = license
                            .getRequirements()
                            .stream()
                            .map(Requirement::getShortText)
                            .findFirst();
                        if (optionalRequirements.isPresent()) {
                            String splitRegex = ";";

                            String requirements = optionalRequirements.get().trim();
                            String[] shortTexts;
                            shortTexts = requirements.split(splitRegex);

                            if (shortTexts.length == 0) {
                                shortTexts = new String[] { requirements };
                            }

                            Set<Requirement> requirementEntities = Arrays
                                .stream(shortTexts)
                                .map(String::trim)
                                .map(requirementRepository::findOneByShortText)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());

                            license.setRequirements(requirementEntities);
                        }
                    }

                    try {
                        License savedLicense = licenseService.saveWithCheck(license);
                        licenseService.createLicenseConflictsForNewLicense(savedLicense);
                        licenseService.createLicenseConflictsForExistingLicenses(savedLicense);
                    } catch (LicenseAlreadyExistException e) {
                        License dbLicense = e.getLicense();
                        dbLicense.updateEmptyFields(license);
                        dbLicense.setRequirements(license.getRequirements());
                    } catch (LicenseException e) {
                        throw new UploadException(e.getMessage());
                    }
                }

                break;
        }
        log.info("Finished processing upload : {}", upload.getId());
    }
}
