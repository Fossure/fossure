import { ILibrary } from '../../../entities/library/library.model';

export interface IDifferenceView {
  sameLibraries?: ILibrary[] | null;
  addedLibraries?: ILibrary[] | null;
  removedLibraries?: ILibrary[] | null;
  firstProjectNewLibraries?: ILibrary[] | null;
  secondProjectNewLibraries?: ILibrary[] | null;
}

export class DifferenceView implements IDifferenceView {
  constructor(
    sameLibraries?: ILibrary[] | null,
    addedLibraries?: ILibrary[] | null,
    removedLibraries?: ILibrary[] | null,
    firstProjectLibraries?: ILibrary[] | null,
    secondProjectLibraries?: ILibrary[] | null
  ) {}
}
