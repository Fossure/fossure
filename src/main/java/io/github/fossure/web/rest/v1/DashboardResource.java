package io.github.fossure.web.rest.v1;

import io.github.fossure.domain.statistics.Dashboard.Insights;
import io.github.fossure.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for communication with Dashboard.
 */
@RestController
@RequestMapping("/api/v1")
public class DashboardResource {

    private final Logger log = LoggerFactory.getLogger(DashboardResource.class);

    private static final String ENTITY_NAME = "dashboard";

    private final DashboardService dashboardService;

    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * {@code GET  /dashboard/insights} : Get insights for dashboard.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the insights,
     * or with status {@code 400 (Bad Request)} if the insights are not available.
     */
    @GetMapping("/dashboard/insights")
    public ResponseEntity<Insights> getInsights() {
        log.debug("REST request to get insights for dashboard");

        Insights insights = dashboardService.getInsights();
        return ResponseEntity.ok().body(insights);
    }
}
