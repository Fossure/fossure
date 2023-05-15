import { Route } from '@angular/router';
import { UserRouteAccessService } from '../core/auth/user-route-access.service';
import { HomeComponent } from './home.component';

export const HOME_ROUTE: Route = {
  path: '',
  component: HomeComponent,
  data: {
    pageTitle: 'Fossure',
  },
  canActivate: [UserRouteAccessService],
};
