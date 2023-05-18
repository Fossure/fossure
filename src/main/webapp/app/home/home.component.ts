import { HttpResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ViewContainerRef } from '@angular/core';
import { Router } from '@angular/router';
import { AccountService } from 'app/core/auth/account.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { UntypedFormBuilder, Validators } from '@angular/forms';
import { Account } from 'app/core/auth/account.model';
import { AlertService } from 'app/core/util/alert.service';
import { NotificationService, NotificationType } from 'app/core/util/notification.service';
import { ILibrary } from 'app/entities/library/library.model';
import { LibraryService } from 'app/entities/library/service/library.service';
import { ILicense } from 'app/entities/license/license.model';
import { LicenseService } from 'app/entities/license/service/license.service';
import { IProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/service/product.service';
import { DESC } from '../config/pagination.constants';
import { IInsights } from './home.model';
import { HomeService } from './home.service';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit, OnDestroy {
  account: Account | null = null;

  libraryFinding?: ILibrary;
  licenseFinding?: ILicense;
  insights: IInsights | null;
  products?: IProduct[];
  libraries?: ILibrary[];
  licenses?: ILicense[];

  librarySearchFormEmptyError = false;
  licenseSearchFormEmptyError = false;

  librarySearchForm = this.fb.group({
    name: [null, Validators.pattern('^(pkg:[a-zA-Z]{1,9}/)?(@?[^/@]+/)?[^@/]+@[^@/]+$')],
    type: [],
  });

  licenseSearchForm = this.fb.group({
    name: [],
  });

  private readonly destroy$ = new Subject<void>();

  constructor(
    protected accountService: AccountService,
    protected router: Router,
    protected homeService: HomeService,
    protected productService: ProductService,
    protected libraryService: LibraryService,
    protected licenseService: LicenseService,
    protected fb: UntypedFormBuilder,
    protected alertService: AlertService,
    protected notificationService: NotificationService,
    protected viewContainerRef: ViewContainerRef
  ) {}

  loadPage(): void {
    this.homeService.getInsights().subscribe((res: HttpResponse<IInsights>) => {
      this.insights = res.body;
    });

    const pageToLoad = 0;
    const pageSize = 5;

    this.productService
      .query({
        'lastUpdatedDate.specified': true, // show only products where the last lastUpdatedDate field is not null
        page: pageToLoad,
        size: pageSize,
        sort: ['lastUpdatedDate' + ',' + DESC, 'name'], // sort first by lastUpdatedDate and than by name
      })
      .subscribe((res: HttpResponse<IProduct[]>) => {
        this.products = res.body ?? [];
      });

    this.libraryService
      .query({
        page: pageToLoad,
        size: pageSize,
        sort: ['createdDate' + ',' + DESC, 'artifactId'], // sort first by createdDate and than by artifactId
      })
      .subscribe((res: HttpResponse<ILibrary[]>) => {
        this.libraries = res.body ?? [];
      });
    this.licenseService
      .query({
        'reviewed.equals': true, // show only reviewed licenses
        page: pageToLoad,
        size: pageSize,
        sort: ['lastReviewedDate' + ',' + DESC, 'fullName'], // sort first by reviewedDate and than by fullName
      })
      .subscribe((res: HttpResponse<ILibrary[]>) => {
        this.licenses = res.body ?? [];
      });
  }

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => (this.account = account));
    this.loadPage();
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  librarySearch(): void {
    this.librarySearchFormEmptyError = false;

    let name = this.librarySearchForm.get(['name'])!.value;

    if (!name || this.librarySearchForm.get(['type'])!.value === null) {
      this.librarySearchFormEmptyError = true;
      return;
    }

    if (name.startsWith('pkg:')) {
      name = name.substring(4);
      const index = name.indexOf('/');
      if (index !== -1) {
        name = name.substring((index as number) + 1);
      }
      this.librarySearchForm.get(['name'])!.setValue(name);
    }

    let groupId = '';
    let artifactId = '';
    let version = '';

    const nameSplitted = name.split('/');
    if (nameSplitted.length === 1) {
      const artifactAndVersion = nameSplitted[0].split('@');
      if (artifactAndVersion.length === 2) {
        artifactId = artifactAndVersion[0];
        version = artifactAndVersion[1];
      } else {
        return;
      }
    } else if (nameSplitted.length === 2) {
      groupId = nameSplitted[0];

      const artifactAndVersion = nameSplitted[1].split('@');
      if (artifactAndVersion.length === 2) {
        artifactId = artifactAndVersion[0];
        version = artifactAndVersion[1];
      } else {
        return;
      }
    }

    this.libraryService
      .query({
        'groupId.equals': groupId ? groupId : null,
        'artifactId.equals': artifactId,
        'version.equals': version,
        'type.equals': this.librarySearchForm.get(['type'])!.value,
      })
      .subscribe({
        next: (res: HttpResponse<ILibrary[]>) => {
          if (res.body?.length) {
            this.notificationService.addNotification({
              title: 'Library search result',
              type: NotificationType.LIBRARY,
              data: res.body[0],
            });
          } else {
            this.notificationService.addNotification({
              title: 'Library search result',
              type: NotificationType.TEXT,
              data: "Sorry, we couldn't find any results for your search. Please check your spelling and try again or it may be that this library does not yet exist in the database.",
            });
          }
        },
        error: () => {
          this.notificationService.addNotification({
            title: 'Library search result',
            type: NotificationType.TEXT,
            data: 'Sorry, a problem has occurred. Please try refining your search.',
          });
        },
      });
  }

  licenseSearch(): void {
    this.licenseSearchFormEmptyError = false;

    if (!this.licenseSearchForm.get(['name'])!.value) {
      this.licenseSearchFormEmptyError = true;
      return;
    }

    this.licenseService
      .query({
        'name.equals': this.licenseSearchForm.get(['name'])!.value,
      })
      .subscribe({
        next: (res: HttpResponse<ILicense[]>) => {
          if (res.body?.length) {
            this.notificationService.addNotification({
              title: 'License search result',
              type: NotificationType.LICENSE,
              data: res.body[0],
            });
          } else {
            this.notificationService.addNotification({
              title: 'License search result',
              type: NotificationType.TEXT,
              data: "Sorry, we couldn't find any results for your search. Please check your spelling and try again or it may be that this license does not yet exist in the database.",
            });
          }
        },
        error: () => {
          this.notificationService.addNotification({
            title: 'License search result',
            type: NotificationType.TEXT,
            data: 'Sorry, a problem has occurred. Please try refining your search.',
          });
        },
      });
  }
}
