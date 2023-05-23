import dayjs from 'dayjs/esm';
import { ILibrary } from 'app/entities/library/library.model';
import { IProject } from 'app/entities/project/project.model';

export interface IDependency {
  id?: number;
  addedDate?: dayjs.Dayjs | null;
  addedManually?: boolean | null;
  hideForPublishing?: boolean | null;
  comment?: string | null;
  library?: ILibrary | null;
  project?: IProject | null;
}

export class Dependency implements IDependency {
  constructor(
    public id?: number,
    public addedDate?: dayjs.Dayjs | null,
    public addedManually?: boolean | null,
    public hideForPublishing?: boolean | null,
    public comment?: string | null,
    public library?: ILibrary | null,
    public project?: IProject | null
  ) {
    this.addedManually = this.addedManually ?? false;
    this.hideForPublishing = this.hideForPublishing ?? false;
  }
}

export function getDependencyIdentifier(dependency: IDependency): number | undefined {
  return dependency.id;
}
