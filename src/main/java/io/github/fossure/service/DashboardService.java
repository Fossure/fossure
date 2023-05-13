package io.github.fossure.service;

import io.github.fossure.domain.statistics.Dashboard;
import io.github.fossure.service.criteria.LibraryCriteria;
import io.github.fossure.service.criteria.LicenseCriteria;
import io.github.fossure.service.criteria.ProductCriteria;
import io.github.fossure.service.criteria.query.LibraryQueryService;
import io.github.fossure.service.criteria.query.LicenseQueryService;
import io.github.fossure.service.criteria.query.ProductQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.jhipster.service.filter.BooleanFilter;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProductService productService;
    private final ProductQueryService productQueryService;
    private final LibraryService libraryService;
    private final LibraryQueryService libraryQueryService;
    private final LicenseService licenseService;
    private final LicenseQueryService licenseQueryService;

    public DashboardService(
        ProductService productService,
        ProductQueryService productQueryService,
        LibraryService libraryService,
        LibraryQueryService libraryQueryService,
        LicenseService licenseService,
        LicenseQueryService licenseQueryService
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
        ProductCriteria productCriteria = new ProductCriteria();
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
