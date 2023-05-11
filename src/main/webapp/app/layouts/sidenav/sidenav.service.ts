import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

@Injectable({
  providedIn: "root"
})
export class SidenavService {
  sidenavCollapsed$: Observable<boolean>;
  private isSidenavCollapsedSource = new Subject<boolean>();

  constructor() {
    this.sidenavCollapsed$ = this.isSidenavCollapsedSource.asObservable();
  }

  toggleSidenav(isCollapsed: boolean): void {
    this.isSidenavCollapsedSource.next(isCollapsed);
  }
}
