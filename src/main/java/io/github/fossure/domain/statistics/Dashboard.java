package io.github.fossure.domain.statistics;

public class Dashboard {

    public static class Insights {

        private Long totalProjects;
        private Long totalActiveProjects;
        private Long totalComponents;
        private Long totalUnidentifiedComponents;
        private Integer percentageReviewedComponents;
        private Long totalLicenses;
        private Integer percentageReviewedLicenses;

        public Insights(
            Long totalProjects,
            Long totalActiveProjects,
            Long totalComponents,
            Long totalUnidentifiedComponents,
            Long totalReviewedComponents,
            Long totalLicenses,
            Long totalReviewedLicenses
        ) {
            this.totalProjects = totalProjects;
            this.totalActiveProjects = totalActiveProjects;
            this.totalComponents = totalComponents;
            this.totalUnidentifiedComponents = totalUnidentifiedComponents;
            this.percentageReviewedComponents = Math.round(totalReviewedComponents * 100.0f / totalComponents);
            this.totalLicenses = totalLicenses;
            this.percentageReviewedLicenses = Math.round(totalReviewedLicenses * 100.0f / totalLicenses);
        }

        public Long getTotalProjects() {
            return totalProjects;
        }

        public void setTotalProjects(Long totalProjects) {
            this.totalProjects = totalProjects;
        }

        public Long getTotalActiveProjects() {
            return totalActiveProjects;
        }

        public void setTotalActiveProjects(Long totalActiveProjects) {
            this.totalActiveProjects = totalActiveProjects;
        }

        public Long getTotalComponents() {
            return totalComponents;
        }

        public void setTotalComponents(Long totalComponents) {
            this.totalComponents = totalComponents;
        }

        public Long getTotalUnidentifiedComponents() {
            return totalUnidentifiedComponents;
        }

        public void setTotalUnidentifiedComponents(Long totalUnidentifiedComponents) {
            this.totalUnidentifiedComponents = totalUnidentifiedComponents;
        }

        public Integer getPercentageReviewedComponents() {
            return percentageReviewedComponents;
        }

        public void setPercentageReviewedComponents(Long totalReviewedComponents, Long totalComponents) {
            this.percentageReviewedComponents = Math.round(totalReviewedComponents * 100.0f / totalComponents);
        }

        public void setPercentageReviewedComponents(Integer percentageReviewedComponents) {
            this.percentageReviewedComponents = percentageReviewedComponents;
        }

        public Long getTotalLicenses() {
            return totalLicenses;
        }

        public void setTotalLicenses(Long totalLicenses) {
            this.totalLicenses = totalLicenses;
        }

        public Integer getPercentageReviewedLicenses() {
            return percentageReviewedLicenses;
        }

        public void setPercentageReviewedLicenses(Long totalReviewedLicenses, Long totalLicenses) {
            this.percentageReviewedLicenses = Math.round(totalReviewedLicenses * 100.0f / totalLicenses);
        }

        public void setPercentageReviewedLicenses(Integer percentageReviewedLicenses) {
            this.percentageReviewedLicenses = percentageReviewedLicenses;
        }

        // prettier-ignore
        @Override
        public String toString() {
            return "Insights{" +
                "totalProjects=" + getTotalProjects() +
                ", totalActiveProjects=" + getTotalActiveProjects() +
                ", totalComponents=" + getTotalComponents() +
                ", totalUnidentifiedComponents=" + getTotalUnidentifiedComponents() +
                ", percentageReviewedComponents=" + getPercentageReviewedComponents() +
                ", totalLicenses=" + getTotalLicenses() +
                ", percentageReviewedLicenses=" + getPercentageReviewedLicenses() +
                "}";
        }
    }
}
