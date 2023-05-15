import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { VERSION } from 'app/app.constants';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { SidenavService } from 'app/layouts/sidenav/sidenav.service';

@Component({
  selector: 'jhi-sidenav',
  templateUrl: './sidenav.component.html'
})
export class SidenavComponent implements OnInit {
  isSidenavCollapsed = true;
  inProduction?: boolean;
  openAPIEnabled?: boolean;
  version = '';
  account: Account | null = null;

  constructor(
      protected loginService: LoginService,
      protected accountService: AccountService,
      protected profileService: ProfileService,
      protected router: Router,
      protected sidenavService: SidenavService
    ) {
      if (VERSION) {
        this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
      }
    }

  ngOnInit(): void {
    const sidenavState = localStorage.getItem('sidenav.collapsed');
    if (sidenavState) {
      this.isSidenavCollapsed = JSON.parse(sidenavState);
    }

    this.profileService.getProfileInfo().subscribe(profileInfo => {
      this.inProduction = profileInfo.inProduction;
      this.openAPIEnabled = profileInfo.openAPIEnabled;
    });

    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
    });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.loginService.logout();
    this.router.navigate(['/login']);
  }

  toggleSidenav(): void {
    this.isSidenavCollapsed = !this.isSidenavCollapsed;
    this.sidenavService.toggleSidenav(this.isSidenavCollapsed);
    localStorage.setItem('sidenav.collapsed', this.isSidenavCollapsed.toString());
  }
}
