import { UploadState } from 'app/entities/enumerations/upload-state.model';
import { IDependency } from 'app/entities/dependency/dependency.model';
import dayjs from 'dayjs/esm';

export interface IProject {
  id?: number;
  name?: string;
  label?: string;
  version?: string;
  createdDate?: dayjs.Dayjs | null;
  lastUpdatedDate?: dayjs.Dayjs | null;
  uploadState?: UploadState | null;
  disclaimer?: string | null;
  delivered?: boolean | null;
  deliveredDate?: dayjs.Dayjs | null;
  contact?: string | null;
  comment?: string | null;
  previousProject?: IProject | null;
  uploadFilter?: string | null;
  libraries?: IDependency[] | null;
}

export class Project implements IProject {
  constructor(
    public id?: number,
    public name?: string,
    public label?: string,
    public version?: string,
    public createdDate?: dayjs.Dayjs | null,
    public lastUpdatedDate?: dayjs.Dayjs | null,
    public uploadState?: UploadState | null,
    public disclaimer?: string | null,
    public delivered?: boolean | null,
    public deliveredDate?: dayjs.Dayjs | null,
    public contact?: string | null,
    public comment?: string | null,
    public previousProject?: IProject | null,
    public uploadFilter?: string | null,
    public libraries?: IDependency[] | null
  ) {
    this.delivered = this.delivered ?? false;
  }
}

export function getProjectIdentifier(project: IProject): number | undefined {
  return project.id;
}
