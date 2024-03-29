import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { LibraryDetailComponent } from '../detail/library-detail.component';
import { LibraryComponent } from '../list/library.component';
import { LibraryUpdateComponent } from '../update/library-update.component';
import { LibraryRoutingResolveService } from './library-routing-resolve.service';

const libraryRoute: Routes = [
  {
    path: '',
    component: LibraryComponent,
    data: {
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: LibraryDetailComponent,
    resolve: {
      library: LibraryRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: LibraryUpdateComponent,
    resolve: {
      library: LibraryRoutingResolveService,
    },
    data: {
      authorities: ['ROLE_USER', 'ROLE_ADMIN'],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: LibraryUpdateComponent,
    resolve: {
      library: LibraryRoutingResolveService,
    },
    data: {
      authorities: ['ROLE_USER', 'ROLE_ADMIN'],
    },
    canActivate: [UserRouteAccessService],
  },
];

@NgModule({
  imports: [RouterModule.forChild(libraryRoute)],
  exports: [RouterModule],
})
export class LibraryRoutingModule {}
