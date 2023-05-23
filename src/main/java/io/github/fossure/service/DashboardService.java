package io.github.fossure.service;

import io.github.fossure.domain.statistics.Dashboard;
import io.github.fossure.service.criteria.LibraryCriteria;
import io.github.fossure.service.criteria.LicenseCriteria;
import io.github.fossure.service.criteria.ProjectCriteria;
import io.github.fossure.service.criteria.query.LibraryQueryService;
import io.github.fossure.service.criteria.query.LicenseQueryService;
import io.github.fossure.service.criteria.query.ProjectQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.jhipster.service.filter.BooleanFilter;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProjectService projectService;
    private final ProjectQueryService projectQueryService;
    private final LibraryService libraryService;
    private final LibraryQueryService libraryQueryService;
    private final LicenseService licenseService;
    private final LicenseQueryService licenseQueryService;

    public DashboardService(
        ProjectService projectService,
        ProjectQueryService projectQueryService,
        LibraryService libraryService,
        LibraryQueryService libraryQueryService,
        LicenseService licenseService,
        LicenseQueryService licenseQueryService
    ) {
        this.projectService = projectService;
        this.projectQueryService = projectQueryService;
        this.libraryService = libraryService;
        this.libraryQueryService = libraryQueryService;
        this.licenseService = licenseService;
        this.licenseQueryService = licenseQueryService;
    }

    public Dashboard.Insights getInsights() {
        log.debug("REST request to get insights for dashboard");
        // Filter to get active projects, reviewed libraries and licenses
        BooleanFilter trueFilter = new BooleanFilter();
        BooleanFilter falseFilter = new BooleanFilter();
        trueFilter.setEquals(true);
        falseFilter.setEquals(false);

        // Count all projects
        long totalProjects = projectService.count();

        // Count all active projects
        ProjectCriteria projectCriteria = new ProjectCriteria();
        projectCriteria.setDelivered(falseFilter);
        long totalActiveProjects = projectQueryService.countByCriteria(projectCriteria);

        // Count all libraries
        long totalLibraries = libraryService.count();

        // Count all libraries with unknown and non-licensed licenses
        long totalUnidentifiedLibraries = libraryService.countByUnidentifiedLicense(
            licenseService.getUnknownLicense(),
            licenseService.getNonLicensedLicense()
        );

        // Count all reviewed libraries
        LibraryCriteria libraryCriteria = new LibraryCriteria();
        libraryCriteria.setReviewed(trueFilter);
        long totalReviewedLibraries = libraryQueryService.countByCriteria(libraryCriteria);

        // Count all licenses
        long totalLicenses = licenseService.count();

        // Count all reviewed licenses
        LicenseCriteria licenseCriteria = new LicenseCriteria();
        licenseCriteria.setReviewed(trueFilter);
        long totalReviewedLicenses = licenseQueryService.countByCriteria(licenseCriteria);

        return new Dashboard.Insights(
            totalProjects,
            totalActiveProjects,
            totalLibraries,
            totalUnidentifiedLibraries,
            totalReviewedLibraries,
            totalLicenses,
            totalReviewedLicenses
        );
    }
}
