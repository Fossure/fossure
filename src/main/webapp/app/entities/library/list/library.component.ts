import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { combineLatest } from 'rxjs';

import { ILibrary } from '../library.model';

import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/config/pagination.constants';
import { DataUtils } from 'app/core/util/data-util.service';
import { LibraryService } from 'app/entities/library/service/library.service';

import { UntypedFormBuilder } from '@angular/forms';
import { IFile } from 'app/core/file/file.model';
import { LibraryDeleteDialogComponent } from 'app/entities/library/delete/library-delete-dialog.component';

@Component({
  selector: 'jhi-library',
  templateUrl: './library.component.html',
})
export class LibraryComponent implements OnInit {
  libraries?: ILibrary[];
  isLoading = false;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page?: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  jsonExport = true;
  isExporting = false;

  searchForm = this.fb.group({
    namespace: [],
    name: [],
    version: [],
    license: [],
    reviewed: [],
  });

  constructor(
    protected libraryService: LibraryService,
    protected activatedRoute: ActivatedRoute,
    protected dataUtils: DataUtils,
    protected router: Router,
    protected modalService: NgbModal,
    protected fb: UntypedFormBuilder
  ) {}

  ngOnInit(): void {
    this.handleNavigation();
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    this.isLoading = true;
    const pageToLoad: number = page ?? this.page ?? 1;

    this.libraryService
      .query({
        'namespace.contains': this.searchForm.get('namespace')?.value ?? null,
        'name.contains': this.searchForm.get('name')?.value ?? null,
        'version.contains': this.searchForm.get('version')?.value ?? null,
        'linkedLicenseShortIdentifier.in': this.searchForm.get('license')?.value ?? null,
        'reviewed.equals': this.searchForm.get('reviewed')?.value ?? null,
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
      })
      .subscribe(
        (res: HttpResponse<ILibrary[]>) => {
          this.isLoading = false;
          this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate);
        },
        () => {
          this.isLoading = false;
          this.onError();
        }
      );
  }

  trackId(index: number, item: ILibrary): number {
    return item.id!;
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    return this.dataUtils.openFile(base64String, contentType);
  }

  delete(library: ILibrary): void {
    const modalRef = this.modalService.open(LibraryDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.library = library;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadPage();
      }
    });
  }

  export(): void {
    this.isExporting = true;
    this.libraryService.export({ format: this.jsonExport ? 'JSON' : 'CSV' }).subscribe(
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
    this.isLoading = true;

    this.libraryService
      .query({
        'namespace.contains': this.searchForm.get('namespace')?.value ?? null,
        'name.contains': this.searchForm.get('name')?.value ?? null,
        'version.contains': this.searchForm.get('version')?.value ?? null,
        'linkedLicenseShortIdentifier.in': this.searchForm.get('license')?.value ?? null,
        sort: this.sort(),
        page: 0,
        size: this.itemsPerPage,
      })
      .subscribe(
        (res: HttpResponse<ILibrary[]>) => {
          this.isLoading = false;
          this.onSuccess(res.body, res.headers, 1, true);
        },
        () => {
          this.isLoading = false;
          this.onError();
        }
      );
  }

  hasErrors(library: ILibrary): boolean {
    return this.libraryService.hasErrors(library);
  }

  protected sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? ASC : DESC)];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  protected handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get('page');
      const pageNumber = +(page ?? 1);
      const sort = (params.get(SORT) ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === ASC;

      const namespace = params.get('namespace.contains');
      const name = params.get('name.contains');
      const version = params.get('version.contains');
      const linkedLicenseShortIdentifier = params.get('linkedLicenseShortIdentifier.in');
      const reviewed = params.get('reviewed.equals');

      this.searchForm.get('namespace')?.setValue(namespace);
      this.searchForm.get('name')?.setValue(name);
      this.searchForm.get('version')?.setValue(version);
      this.searchForm.get('license')?.setValue(linkedLicenseShortIdentifier);
      this.searchForm.get('reviewed')?.setValue(reviewed);

      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    });
  }

  protected onSuccess(data: ILibrary[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/library'], {
        queryParams: {
          'namespace.contains': this.searchForm.get('namespace')?.value ?? null,
          'name.contains': this.searchForm.get('name')?.value ?? null,
          'version.contains': this.searchForm.get('version')?.value ?? null,
          'linkedLicenseShortIdentifier.in': this.searchForm.get('license')?.value ?? null,
          'reviewed.equals': this.searchForm.get('reviewed')?.value ?? null,
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? ASC : DESC),
        },
      });
    }
    this.libraries = data ?? [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
