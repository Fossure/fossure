import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'firstCapitalLetter',
})
export class FirstCapitalLetterPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (value) {
      return Array.from(value.toUpperCase())[0];
    } else {
      return "";
    }
  }
}
