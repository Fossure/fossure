import { Route } from '@angular/router';
import { SidenavComponent } from './sidenav.component';

export const sidenavRoute: Route = {
  path: '',
  component: SidenavComponent,
  outlet: 'sidenav',
};
