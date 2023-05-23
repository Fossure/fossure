import { NgModule } from '@angular/core';
import { ProjectDeleteDialogComponent } from 'app/entities/project/delete/project-delete-dialog.component';
import { ProjectDetailComponent } from 'app/entities/project/detail/project-detail.component';
import { ProjectComponent } from 'app/entities/project/list/project.component';
import { ProjectUpdateComponent } from 'app/entities/project/update/project-update.component';
import { SharedModule } from 'app/shared/shared.module';
import { ProjectRoutingModule } from './route/project-routing.module';

import { NgxChartsModule } from '@swimlane/ngx-charts';
import { AbsoluteNumberPipe } from 'app/shared/number/absolute-number.pipe';
import { ProjectOverviewModule } from 'app/shared/statistics/project-overview/project-overview.module';
import { DifferenceViewComponent } from '../../shared/modals/difference-view-modal/difference-view.component';
import { ProjectUpdateLibraryComponent } from './update-library/project-update-library.component';

@NgModule({
  imports: [SharedModule, ProjectRoutingModule, NgxChartsModule, ProjectOverviewModule],
  declarations: [
    ProjectComponent,
    ProjectDetailComponent,
    ProjectUpdateComponent,
    ProjectDeleteDialogComponent,
    AbsoluteNumberPipe,
    ProjectUpdateLibraryComponent,
    DifferenceViewComponent,
  ],
  entryComponents: [ProjectDeleteDialogComponent],
})
export class ProjectModule {}
