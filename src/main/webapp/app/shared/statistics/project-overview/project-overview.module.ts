import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectOverviewComponent } from './project-overview.component';
import { NgxChartsModule } from '@swimlane/ngx-charts';

@NgModule({
  declarations: [ProjectOverviewComponent],
  imports: [CommonModule, NgxChartsModule],
  exports: [ProjectOverviewComponent],
})
export class ProjectOverviewModule {}
