import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { combineLatest } from 'rxjs';

import { UntypedFormBuilder } from '@angular/forms';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/config/pagination.constants';
import { DataUtils } from 'app/core/util/data-util.service';
import { ProjectDeleteDialogComponent } from 'app/entities/project/delete/project-delete-dialog.component';
import { ProjectService } from 'app/entities/project/service/project.service';
import { DifferenceViewComponent } from '../../../shared/modals/difference-view-modal/difference-view.component';
import { IProject } from '../project.model';

@Component({
  selector: 'jhi-project',
  templateUrl: './project.component.html',
})
export class ProjectComponent implements OnInit {
  projects?: IProject[];
  isLoading = false;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page?: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;

  @ViewChild(DifferenceViewComponent, { static: true })
  differenceView?: DifferenceViewComponent;

  searchForm = this.fb.group({
    name: [],
    label: [],
    version: [],
    hideDelivered: true,
  });

  constructor(
    protected projectService: ProjectService,
    protected activatedRoute: ActivatedRoute,
    protected dataUtils: DataUtils,
    protected router: Router,
    protected modalService: NgbModal,
    protected fb: UntypedFormBuilder
  ) {}

  ngOnInit(): void {
    this.handleNavigation();
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        // close all open modals
        this.modalService.dismissAll();
      }
    });
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    this.isLoading = true;
    const pageToLoad: number = page ?? this.page ?? 1;

    this.projectService
      .query({
        'name.contains': this.searchForm.get('name')?.value ?? null,
        'label.contains': this.searchForm.get('label')?.value ?? null,
        'version.contains': this.searchForm.get('version')?.value ?? null,
        'delivered.equals': this.searchForm.get('hideDelivered')?.value === true ? false : null,
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
      })
      .subscribe({
        next: (res: HttpResponse<IProject[]>) => {
          this.isLoading = false;
          this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate);
        },
        error: () => {
          this.isLoading = false;
          this.onError();
        },
      });
  }

  trackId(index: number, item: IProject): number {
    return item.id!;
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    return this.dataUtils.openFile(base64String, contentType);
  }

  delete(project: IProject): void {
    const modalRef = this.modalService.open(ProjectDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.project = project;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadPage();
      }
    });
  }

  search(): void {
    this.loadPage(1, false);
  }

  openDifferenceViewModal(content: any): void {
    this.modalService.open(content, {
      scrollable: true,
      size: 'xl',
    });
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

      const name = params.get('name');
      const label = params.get('label');
      const version = params.get('version');
      const hideDelivered = params.get('hideDelivered');

      this.searchForm.get('name')?.setValue(name);
      this.searchForm.get('label')?.setValue(label);
      this.searchForm.get('version')?.setValue(version);

      if (hideDelivered === 'false') {
        this.searchForm.get('hideDelivered')?.setValue(false);
      }

      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    });
  }

  protected onSuccess(data: IProject[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/project'], {
        queryParams: {
          name: this.searchForm.get('name')?.value ?? null,
          label: this.searchForm.get('label')?.value ?? null,
          version: this.searchForm.get('version')?.value ?? null,
          hideDelivered: this.searchForm.get('hideDelivered')?.value === true ? null : false,
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? ASC : DESC),
        },
      });
    }
    this.projects = data ?? [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
