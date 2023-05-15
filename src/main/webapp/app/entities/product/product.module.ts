import { NgModule } from '@angular/core';
import { ProductDeleteDialogComponent } from 'app/entities/product/delete/product-delete-dialog.component';
import { ProductDetailComponent } from 'app/entities/product/detail/product-detail.component';
import { ProductComponent } from 'app/entities/product/list/product.component';
import { ProductUpdateComponent } from 'app/entities/product/update/product-update.component';
import { SharedModule } from 'app/shared/shared.module';
import { ProductRoutingModule } from './route/product-routing.module';

import { NgxChartsModule } from '@swimlane/ngx-charts';
import { AbsoluteNumberPipe } from 'app/shared/number/absolute-number.pipe';
import { ProductOverviewModule } from 'app/shared/statistics/product-overview/product-overview.module';
import { DifferenceViewComponent } from '../../shared/modals/difference-view-modal/difference-view.component';
import { ProductUpdateLibraryComponent } from './update-library/product-update-library.component';

@NgModule({
  imports: [SharedModule, ProductRoutingModule, NgxChartsModule, ProductOverviewModule],
  declarations: [
    ProductComponent,
    ProductDetailComponent,
    ProductUpdateComponent,
    ProductDeleteDialogComponent,
    AbsoluteNumberPipe,
    ProductUpdateLibraryComponent,
    DifferenceViewComponent,
  ],
  entryComponents: [ProductDeleteDialogComponent],
})
export class ProductModule {}
