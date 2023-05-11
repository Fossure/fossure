export interface IInsights {
  totalProjects: number;
  totalActiveProjects: number;
  totalComponents: number;
  percentageReviewedComponents: number;
  totalUnidentifiedComponents: number;
  totalLicenses: number;
  percentageReviewedLicenses: number;
}

export class Insights {
  constructor(
    public totalProjects: number,
    public totalActiveProjects: number,
    public totalComponents: number,
    public percentageReviewedComponents: number,
    public totalUnidentifiedComponents: number,
    public totalLicenses: number,
    public percentageReviewedLicenses: number
  ) {}
}
