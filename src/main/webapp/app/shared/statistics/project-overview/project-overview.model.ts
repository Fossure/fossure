export interface IProjectOverview {
  numberOfLibraries?: number | null;
  numberOfLibrariesPrevious?: number | null;
  numberOfLicenses?: number | null;
  numberOfLicensesPrevious?: number | null;
  reviewedLibraries?: number | null;
}

export class ProjectOverview implements IProjectOverview {
  constructor(
    public numberOfLibraries?: number | null,
    public numberOfLibrariesPrevious?: number | null,
    public numberOfLicenses?: number | null,
    public numberOfLicensesPrevious?: number | null,
    public reviewedLibraries?: number | null
  ) {}
}
