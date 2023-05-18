import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router, ActivatedRouteSnapshot, NavigationEnd } from '@angular/router';
import { SidenavService } from 'app/layouts/sidenav/sidenav.service'
import { AccountService } from 'app/core/auth/account.service';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html'
})
export class MainComponent implements OnInit {
  isSidenavCollapsed = true;

  constructor(
    private accountService: AccountService,
    private titleService: Title,
    private router: Router,
    private sidenavService: SidenavService
  ) {}

  ngOnInit(): void {
    const sidenavState = localStorage.getItem('sidenav.collapsed');
    if (sidenavState) {
      this.isSidenavCollapsed = JSON.parse(sidenavState);
    }

    // try to log in automatically
    this.accountService.identity().subscribe();

    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updateTitle();
      }
    });

    this.sidenavService.sidenavCollapsed$.subscribe(isCollapsed => {
      this.isSidenavCollapsed = isCollapsed;
    });
  }


  private getPageTitle(routeSnapshot: ActivatedRouteSnapshot): string {
    const title: string = routeSnapshot.data['pageTitle'] ?? '';
    if (routeSnapshot.firstChild) {
      return this.getPageTitle(routeSnapshot.firstChild) || title;
    }
    return title;
  }

  private updateTitle(): void {
    let pageTitle = this.getPageTitle(this.router.routerState.snapshot.root);
    if (!pageTitle) {
      pageTitle = 'Fossure';
    }
    this.titleService.setTitle(pageTitle);
  }
}
