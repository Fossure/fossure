import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { DependencyDeleteDialogComponent } from './delete/dependency-delete-dialog.component';

@NgModule({
  imports: [SharedModule],
  declarations: [DependencyDeleteDialogComponent],
  entryComponents: [DependencyDeleteDialogComponent],
})
export class DependencyModule {}
