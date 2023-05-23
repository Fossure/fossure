import { Pipe, PipeTransform } from '@angular/core';
import { ILibrary } from '../../entities/library/library.model';

@Pipe({
  name: 'libraryLabel',
})
export class LibraryLabelPipe implements PipeTransform {
  transform(library: ILibrary): string {
    if (library.namespace) {
      return `${library.namespace}:${library.name!}:${library.version!}`;
    } else {
      return `${library.name!}:${library.version!}`;
    }
  }
}
