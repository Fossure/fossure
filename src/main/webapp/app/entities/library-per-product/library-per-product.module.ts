import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { LibraryPerProductDeleteDialogComponent } from './delete/library-per-product-delete-dialog.component';

@NgModule({
  imports: [SharedModule],
  declarations: [LibraryPerProductDeleteDialogComponent],
  entryComponents: [LibraryPerProductDeleteDialogComponent],
})
export class LibraryPerProductModule {}
