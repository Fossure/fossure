import { ICountOccurrences } from '../count-occurrences.model';
import { ISeries } from '../series.model';

export interface IProjectStatistic {
  licenseDistribution?: ICountOccurrences[] | null;
  licenseRiskDistribution?: ICountOccurrences[] | null;
  licenseRiskSeries?: ISeries[] | null;
  numberOfLibrariesSeries?: ISeries[] | null;
}

export class ProjectStatistic implements IProjectStatistic {
  constructor(
    public licenseDistribution?: ICountOccurrences[] | null,
    public licenseRiskDistribution?: ICountOccurrences[] | null,
    public licenseRiskSeries?: ISeries[] | null,
    public numberOfLibrariesSeries?: ISeries[] | null
  ) {}
}
