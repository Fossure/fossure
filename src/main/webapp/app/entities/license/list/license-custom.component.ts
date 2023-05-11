import { Component, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ILicense } from '../license.model';
import { DataUtils } from 'app/core/util/data-util.service';

import { IFile } from 'app/core/file/file.model';
import { IRequirement } from 'app/entities/requirement/requirement.model';
import { UntypedFormBuilder } from '@angular/forms';
import { LicenseDeleteDialogCustomComponent } from 'app/entities/license/delete/license-delete-dialog-custom.component';
import { LicenseCustomService } from 'app/entities/license/service/license-custom.service';
import { LicenseComponent } from 'app/entities/license/list/license.component';
import { RequirementCustomService } from 'app/entities/requirement/service/requirement-custom.service';
import { ASC, SORT } from '../../../config/pagination.constants';
import { ILicenseRisk } from 'app/entities/license-risk/license-risk.model';
import { LicenseRiskCustomService } from 'app/entities/license-risk/service/license-risk-custom.service';

@Component({
  selector: 'jhi-license-custom',
  templateUrl: './license-custom.component.html',
})
export class LicenseCustomComponent extends LicenseComponent implements OnInit {
  jsonExport = true;
  isExporting = false;

  requirementsSharedCollection: (string | undefined)[] = [];
  licenseRisks: ILicenseRisk[] | null = [];

  searchForm = this.fb.group({
    fullName: [],
    shortIdentifier: [],
    spdx: [],
    requirements: [],
    reviewed: false,
    unreviewed: false,
  });

  filterForm = this.fb.group({
    reviewed: false,
    unreviewed: false,
    reviewer: [],
  });

  riskForm = this.fb.group({
    '1': false,
    '2': false,
    '3': false,
    '4': false,
    '5': false,
    '6': false,
    '7': false,
  });

  constructor(
    protected licenseService: LicenseCustomService,
    protected requirementService: RequirementCustomService,
    protected licenseRiskService: LicenseRiskCustomService,
    protected activatedRoute: ActivatedRoute,
    protected dataUtils: DataUtils,
    protected router: Router,
    protected modalService: NgbModal,
    protected fb: UntypedFormBuilder
  ) {
    super(licenseService, activatedRoute, dataUtils, router, modalService);
  }

  ngOnInit(): void {
    super.ngOnInit();

    this.licenseRiskService
      .query({
        sort: ['level', 'asc'],
        page: 0,
        size: 10,
      })
      .subscribe((res: HttpResponse<ILicenseRisk[]>) => {
        this.licenseRisks = res.body ?? [];
      });
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    this.isLoading = true;
    const pageToLoad: number = page ?? this.page ?? 1;

    this.licenseService.query(this.buildSearchAndFilterParams(pageToLoad - 1)).subscribe(
      (res: HttpResponse<ILicense[]>) => {
        this.isLoading = false;
        this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate);
      },
      () => {
        this.isLoading = false;
        super.onError();
      }
    );

    this.updateForm();
  }

  delete(license: ILicense): void {
    const modalRef = this.modalService.open(LicenseDeleteDialogCustomComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.license = license;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadPage();
      }
    });
  }

  trackRequirementById(index: number, item: IRequirement): number {
    return item.id!;
  }

  export(): void {
    this.isExporting = true;
    this.licenseService.export({ format: this.jsonExport ? 'JSON' : 'CSV' }).subscribe(
      (data: HttpResponse<IFile>) => {
        if (data.body) {
          this.dataUtils.downloadFile(data.body.fileContentType, data.body.file, data.body.fileName);
          this.isExporting = false;
        }
      },
      () => (this.isExporting = false)
    );
  }

  search(): void {
    this.licenseService.query(this.buildSearchAndFilterParams(0)).subscribe({
      next: (res: HttpResponse<ILicense[]>) => {
        this.isLoading = false;
        this.onSuccess(res.body, res.headers, 1, true);
      },
      error: () => {
        this.isLoading = false;
        super.onError();
      },
    });
  }

  protected handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get('page');
      const pageNumber = +(page ?? 1);
      const sort = (params.get(SORT) ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === ASC;

      const fullName = params.get('fullName.contains');
      const shortIdentifier = params.get('shortIdentifier.contains');
      const spdxIdentifier = params.get('spdxIdentifier.contains');
      const requirements = params.get('requirementShortText.equals');
      const reviewed = params.get('reviewed.equals');
      const unreviewed = params.get('unreviewed.equals');
      const reviewer = params.get('lastReviewedByLogin.equals');
      const licenseRiskId = params.get('licenseRiskId.in')?.split(',');

      this.searchForm.get('fullName')?.setValue(fullName);
      this.searchForm.get('shortIdentifier')?.setValue(shortIdentifier);
      this.searchForm.get('spdxIdentifier')?.setValue(spdxIdentifier);
      this.searchForm.get('requirements')?.setValue(requirements);
      this.searchForm.get('reviewer')?.setValue(reviewer);

      if (reviewed === 'true') {
        this.filterForm.get('reviewed')?.setValue(true);
      }

      if (unreviewed === 'true') {
        this.filterForm.get('unreviewed')?.setValue(true);
      }

      Object.keys(this.riskForm.controls).forEach(key => {
        if (licenseRiskId?.includes(key)) {
          this.riskForm.get(key)?.setValue(true);
        } else {
          this.riskForm.get(key)?.setValue(false);
        }
      });

      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    });
  }

  protected onSuccess(data: ILicense[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/license'], {
        queryParams: this.buildSearchAndFilterParams(page),
      });
    }
    this.licenses = data ?? [];
    this.ngbPaginationPage = this.page;
  }

  protected updateForm(): void {
    this.requirementService
      .query()
      .subscribe((res: HttpResponse<IRequirement[]>) => (this.requirementsSharedCollection = res.body?.map(e => e.shortText) ?? []));
  }

  private buildSearchAndFilterParams(page: number): any {
    const params = {
      page,
      size: this.itemsPerPage,
      sort: this.sort(),
    };

    params['fullName.contains'] = this.searchForm.get('fullName')?.value ?? null;
    params['shortIdentifier.contains'] = this.searchForm.get('shortIdentifier')?.value ?? null;
    params['spdxIdentifier.contains'] = this.searchForm.get('spdx')?.value ?? null;
    params['requirementShortText.equals'] =
      this.searchForm.get('requirements')?.value && this.searchForm.get('requirements')?.value !== 'null'
        ? this.searchForm.get('requirements')?.value
        : null;

    params['lastReviewedByLogin.equals'] = this.filterForm.get('reviewer')?.value ?? null;
    
    if (this.filterForm.get('reviewed')?.value === true && this.filterForm.get('unreviewed')?.value === true) {
      params['reviewed.equals'] = null;
      this.filterForm.get('reviewed')?.setValue(false);
      this.filterForm.get('unreviewed')?.setValue(false);
    } else if (this.filterForm.get('reviewed')?.value === true) {
      params['reviewed.equals'] = true;
    } else if (this.filterForm.get('unreviewed')?.value === true) {
      params['reviewed.equals'] = false;
    }

    params['licenseRiskId.in'] = this.createLicenseRiskFilter()?.join(',') ?? null;

    return params;
  }

  private createLicenseRiskFilter(): string[] | null {
    const filter = Object.keys(this.riskForm.controls).filter(key => this.riskForm.get(key)?.value === true);

    if (filter.length === 7) {
      return null;
    }

    return filter;
  }
}
