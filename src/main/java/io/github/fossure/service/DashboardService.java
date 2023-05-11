package io.github.fossure.service;

import io.github.fossure.domain.statistics.Dashboard;
import io.github.fossure.service.criteria.LibraryCustomCriteria;
import io.github.fossure.service.criteria.LicenseCustomCriteria;
import io.github.fossure.service.criteria.ProductCustomCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.jhipster.service.filter.BooleanFilter;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProductCustomService productService;
    private final ProductQueryCustomService productQueryService;
    private final LibraryCustomService libraryService;
    private final LibraryQueryCustomService libraryQueryService;
    private final LicenseCustomService licenseService;
    private final LicenseQueryCustomService licenseQueryService;

    public DashboardService(
        ProductCustomService productService,
        ProductQueryCustomService productQueryService,
        LibraryCustomService libraryService,
        LibraryQueryCustomService libraryQueryService,
        LicenseCustomService licenseService,
        LicenseQueryCustomService licenseQueryService
    ) {
        this.productService = productService;
        this.productQueryService = productQueryService;
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
        long totalProjects = productService.count();

        // Count all active projects
        ProductCustomCriteria productCriteria = new ProductCustomCriteria();
        productCriteria.setDelivered(falseFilter);
        long totalActiveProjects = productQueryService.countByCriteria(productCriteria);

        // Count all libraries
        long totalLibraries = libraryService.count();

        // Count all libraries with unknown and non-licensed licenses
        long totalUnidentifiedLibraries = libraryService.countByUnidentifiedLicense(
            licenseService.getUnknownLicense(),
            licenseService.getNonLicensedLicense()
        );

        // Count all reviewed libraries
        LibraryCustomCriteria libraryCriteria = new LibraryCustomCriteria();
        libraryCriteria.setReviewed(trueFilter);
        long totalReviewedLibraries = libraryQueryService.countByCriteria(libraryCriteria);

        // Count all licenses
        long totalLicenses = licenseService.count();

        // Count all reviewed licenses
        LicenseCustomCriteria licenseCriteria = new LicenseCustomCriteria();
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
