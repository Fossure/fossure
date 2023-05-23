import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, OnInit, QueryList, TemplateRef, ViewChild, ViewChildren } from '@angular/core';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest } from 'rxjs';

import { NgbModal, NgbOffcanvas } from '@ng-bootstrap/ng-bootstrap';

import { File, IFile } from 'app/core/file/file.model';
import { Upload } from 'app/core/upload/upload.model';
import { DataUtils, FileLoadError } from 'app/core/util/data-util.service';
import { EventManager, EventWithContent } from 'app/core/util/event-manager.service';
import { UploadState } from 'app/entities/enumerations/upload-state.model';
import { IDependency, Dependency } from 'app/entities/dependency/dependency.model';
import { ILibrary } from 'app/entities/library/library.model';
import { ILicenseRisk } from 'app/entities/license-risk/license-risk.model';
import { AlertError } from 'app/shared/alert/alert-error.model';
import { ICountOccurrences } from 'app/shared/statistics/count-occurrences.model';
import { IProjectOverview } from 'app/shared/statistics/project-overview/project-overview.model';
import { IProject } from '../project.model';

import { AlertService } from 'app/core/util/alert.service';

import { DATE_FORMAT } from 'app/config/input.constants';
import { NO_COPYRIGHT } from 'app/config/library-field.constants';
import { DependencyDeleteDialogComponent } from 'app/entities/dependency/delete/dependency-delete-dialog.component';
import { DependencyService } from 'app/entities/dependency/service/dependency.service';
import { LibraryService } from 'app/entities/library/service/library.service';
import { LicenseRiskService } from 'app/entities/license-risk/service/license-risk.service';
import { ProjectService } from 'app/entities/project/service/project.service';
import { UploadService } from 'app/entities/upload/service/upload.service';
import { finalize } from 'rxjs/operators';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from '../../../config/pagination.constants';
import { DifferenceViewComponent } from '../../../shared/modals/difference-view-modal/difference-view.component';
import { IProjectStatistic } from '../../../shared/statistics/project-overview/project-statistic.model';
import { ProjectUpdateLibraryComponent } from '../update-library/project-update-library.component';

@Component({
  selector: 'jhi-project-detail',
  templateUrl: './project-detail.component.html',
})
export class ProjectDetailComponent implements OnInit, AfterViewInit {
  @ViewChild(ProjectUpdateLibraryComponent)
  projectUpdateLibraryComponent?: ProjectUpdateLibraryComponent;

  @ViewChildren(DifferenceViewComponent)
  differenceView: QueryList<DifferenceViewComponent>;

  no_copyright = NO_COPYRIGHT;

  project: IProject | null = null;
  dependencies?: IDependency[] | null;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page?: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  uploadStateType = UploadState.OK;
  isLoading = false;
  isLoadingAddSearch = false;
  isDownloadingOss = false;
  isCreatingArchive = false;
  isDownloadingZip = false;
  isTransferring = false;
  ossFormat = 'CSV';
  ossType = 'DEFAULT';
  archiveFormat = 'FULL';
  archiveShipment = 'DOWNLOAD';
  showMoreOss = false;
  showMoreSca = false;
  isCollapsedUploadFilter = true;
  isCollapsedUploadWithAuthentication = true;
  growthLibraries?: number | undefined;
  growthLicenses?: number | undefined;
  percentageReviewedLibraries?: number | undefined;
  overview?: IProjectOverview | null;
  statistic?: IProjectStatistic | null;
  licenseRisks: ILicenseRisk[] | null = [];
  licenseDistribution: ICountOccurrences[] = [];
  addSearchLibraries?: ILibrary[];

  dtn = 0;

  isSaving = false;
  saved = false;
  dependency?: IDependency;
  dependencyForm = this.fb.group({
    hide: [],
    localComment: [],
  });

  tableForm = this.fb.group({
    groupId: true,
    artifactId: true,
    version: true,
    type: false,
    licenses: false,
    licenseToPublish: true,
    risk: true,
    licenseUrl: false,
    licenseText: true,
    sourceCodeUrl: false,
    copyright: false,
    reviewed: false,
    hideForPublishing: false,
    localComment: false,
    errorLog: false,
    manuallyAdded: false,
  });

  uploadForm = this.fb.group({
    url: [],
    username: [],
    password: [],
    file: [],
    fileContentType: [],
    fileName: [],
    additionalLibraries: [],
    additionalLibrariesContentType: [],
    additionalLibrariesFileName: [],
    deleteData: true,
  });

  addLibraryForm = this.fb.group({
    groupId: [],
    artifactId: [],
    version: [],
    libraries: [],
  });

  searchForm = this.fb.group({
    artifactId: [],
    licenses: [],
  });

  nextProjectForm = this.fb.group({
    version: [null, [Validators.required]],
    delivered: true,
    copy: true,
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

  filterForm = this.fb.group({
    licenseConflicts: false,
    newLibrariesSinceLastUpload: false,
  });

  constructor(
    protected projectService: ProjectService,
    protected libraryService: LibraryService,
    protected dependencyService: DependencyService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected dataUtils: DataUtils,
    protected alertService: AlertService,
    protected fb: UntypedFormBuilder,
    protected modalService: NgbModal,
    protected eventManager: EventManager,
    protected uploadService: UploadService,
    protected licenseRiskService: LicenseRiskService,
    protected offcanvasService: NgbOffcanvas
  ) {}

  ngAfterViewInit(): void {
    this.differenceView.changes.subscribe((comps: QueryList<DifferenceViewComponent>) => {
      comps.get(0)?.compareWithParameters(this.project?.previousProject?.id, this.project?.id);
    });
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    if (this.project?.id) {
      this.projectService.find(this.project.id).subscribe(
        (res: HttpResponse<IProject>) => {
          this.onProjectSuccess(res.body);
        },
        () => {
          this.onError();
        }
      );

      this.loadLibraries(page, dontNavigate);

      this.loadOverview();
    }
  }

  loadLibraries(page?: number, dontNavigate?: boolean): void {
    if (this.project?.id ) {
      this.isLoading = true;
      const pageToLoad: number = page ?? this.page ?? 1;
      const licenseRiskFilter = this.createLicenseRiskFilter();

      this.dependencyService
        .query({
          'projectId.equals': this.project.id,
          'artifactId.contains': this.searchForm.get('artifactId')?.value ?? null,
          'licensesShortIdentifier.contains': this.searchForm.get('licenses')?.value ?? null,
          'libraryRiskId.in': licenseRiskFilter?.join(',') ?? null,
          'errorLogMessage.contains': this.filterForm.get('licenseConflicts')!.value ? 'License Conflict' : null,
          'errorLogStatus.notEquals': this.filterForm.get('licenseConflicts')!.value ? 'CLOSED' : null,
          'libraryCreatedDate.greaterThanOrEqual': this.filterForm.get('newLibrariesSinceLastUpload')!.value
            ? this.project.lastUpdatedDate?.format(DATE_FORMAT)
            : null,
          page: pageToLoad - 1,
          size: this.itemsPerPage,
          sort: this.sort(),
        })
        .subscribe(
          (res: HttpResponse<IDependency[]>) => {
            this.isLoading = false;
            this.onLLPSuccess(res.body, res.headers, pageToLoad, !dontNavigate);
          },
          () => {
            this.isLoading = false;
            this.onError();
          }
        );
    }
  }

  loadOverview(): void {
    if (this.project?.id) {
      this.projectService.overview(this.project.id).subscribe((res: HttpResponse<IProjectOverview>) => {
        this.overview = res.body;

        if (this.overview?.reviewedLibraries != null && this.overview.numberOfLibraries != null) {
          if (this.overview.numberOfLibraries === 0) {
            this.percentageReviewedLibraries = 0;
          } else {
            this.percentageReviewedLibraries = +((this.overview.reviewedLibraries / this.overview.numberOfLibraries) * 100).toFixed();
          }
        }

        if (this.project?.previousProject) {
          if (this.overview?.numberOfLibraries != null && this.overview.numberOfLibrariesPrevious != null) {
            if (this.overview.numberOfLibraries === 0 && this.overview.numberOfLibrariesPrevious === 0) {
              this.growthLibraries = 0;
            } else if (this.overview.numberOfLibraries !== 0 && this.overview.numberOfLibrariesPrevious === 0) {
              this.growthLibraries = +((this.overview.numberOfLibraries / 1 - 1) * 100).toFixed();
            } else {
              this.growthLibraries = +((this.overview.numberOfLibraries / this.overview.numberOfLibrariesPrevious - 1) * 100).toFixed();
            }
          }
          if (this.overview?.numberOfLicenses != null && this.overview.numberOfLicensesPrevious != null) {
            if (this.overview.numberOfLicenses === 0 && this.overview.numberOfLicensesPrevious === 0) {
              this.growthLicenses = 0;
            } else if (this.overview.numberOfLicenses !== 0 && this.overview.numberOfLicensesPrevious === 0) {
              this.growthLicenses = +((this.overview.numberOfLicenses / 1 - 1) * 100).toFixed();
            } else {
              this.growthLicenses = +((this.overview.numberOfLicenses / this.overview.numberOfLicensesPrevious - 1) * 100).toFixed();
            }
          }
        }
      });
    }
  }

  loadStatistics(): void {
    if (this.project?.id) {
      this.projectService.overview(this.project.id).subscribe((res: HttpResponse<IProjectOverview>) => {
        this.overview = res.body;

        if (this.overview?.reviewedLibraries != null && this.overview.numberOfLibraries != null) {
          if (this.overview.numberOfLibraries === 0) {
            this.percentageReviewedLibraries = 0;
          } else {
            this.percentageReviewedLibraries = +((this.overview.reviewedLibraries / this.overview.numberOfLibraries) * 100).toFixed();
          }
        }

        if (this.project?.previousProject) {
          if (this.overview?.numberOfLibraries != null && this.overview.numberOfLibrariesPrevious != null) {
            if (this.overview.numberOfLibraries === 0 && this.overview.numberOfLibrariesPrevious === 0) {
              this.growthLibraries = 0;
            } else if (this.overview.numberOfLibraries !== 0 && this.overview.numberOfLibrariesPrevious === 0) {
              this.growthLibraries = +((this.overview.numberOfLibraries / 1 - 1) * 100).toFixed();
            } else {
              this.growthLibraries = +((this.overview.numberOfLibraries / this.overview.numberOfLibrariesPrevious - 1) * 100).toFixed();
            }
          }
          if (this.overview?.numberOfLicenses != null && this.overview.numberOfLicensesPrevious != null) {
            if (this.overview.numberOfLicenses === 0 && this.overview.numberOfLicensesPrevious === 0) {
              this.growthLicenses = 0;
            } else if (this.overview.numberOfLicenses !== 0 && this.overview.numberOfLicensesPrevious === 0) {
              this.growthLicenses = +((this.overview.numberOfLicenses / 1 - 1) * 100).toFixed();
            } else {
              this.growthLicenses = +((this.overview.numberOfLicenses / this.overview.numberOfLicensesPrevious - 1) * 100).toFixed();
            }
          }
        }
      });
    }
  }

  ngOnInit(): void {
    this.router.routeReuseStrategy.shouldReuseRoute = () => false;
    this.router.onSameUrlNavigation = 'reload';

    this.loadLocalStorage();
    this.onLibraryTableChanges();

    this.activatedRoute.data.subscribe(({ project }) => {
      this.project = project;
      this.uploadStateType = this.project?.uploadState ?? UploadState.OK;
    });

    this.licenseRiskService
      .query({
        sort: ['level', 'asc'],
        page: 0,
        size: 10,
      })
      .subscribe((res: HttpResponse<ILicenseRisk[]>) => {
        this.licenseRisks = res.body ?? [];
      });

    this.handleNavigation();
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  previousState(): void {
    window.history.back();
  }

  downloadOssList(): void {
    this.isDownloadingOss = true;
    if (this.project?.id) {
      this.projectService.oss(this.project.id, { format: this.ossFormat, type: this.ossType }).subscribe(
        (res: HttpResponse<IFile>) => {
          if (res.body) {
            this.dataUtils.downloadFile(res.body.fileContentType, res.body.file, res.body.fileName);
            this.isDownloadingOss = false;
          }
        },
        () => (this.isDownloadingOss = false)
      );
    }
  }

  createArchive(): void {
    this.isCreatingArchive = true;
    if (this.project?.id) {
      this.projectService
        .archive(this.project.id, {
          format: this.archiveFormat,
          shipment: this.archiveShipment,
        })
        .subscribe(
          (res: HttpResponse<IFile>) => {
            if (res.body) {
              this.dataUtils.downloadFile(res.body.fileContentType, res.body.file, res.body.fileName);
            } else {
              const platform = res.headers.get('PLATFORM')!;
              this.alertService.addAlert({
                type: 'success',
                message: platform,
                timeout: 0,
                toast: false,
              });
            }
            const complete = res.headers.get('COMPLETE');
            if (complete) {
              this.alertService.addAlert({
                type: 'danger',
                message: complete,
                timeout: 0,
                toast: false,
              });
            }
            this.isCreatingArchive = false;
          },
          () => (this.isCreatingArchive = false)
        );
    }
  }

  downloadZip(): void {
    this.isDownloadingZip = true;
    if (this.project?.id) {
      this.projectService.zip(this.project.id).subscribe(
        (res: HttpResponse<IFile>) => {
          if (res.body) {
            this.dataUtils.downloadFile(res.body.fileContentType, res.body.file, res.body.fileName);
            this.isDownloadingZip = false;
          }
        },
        () => (this.isDownloadingZip = false)
      );
    }
  }

  openScrollableContent(longContent: TemplateRef<any>, windowSize: string): void {
    this.modalService.open(longContent, { scrollable: true, size: windowSize });
  }

  open(content: TemplateRef<any>): void {
    this.modalService.open(content).result.then(
      () => {
        // Empty
      },
      () => {
        // Empty
      }
    );
  }

  openSide(content: TemplateRef<any>, dep: IDependency): void {
    this.saved = false;
    this.dependency = dep;
    this.updateForm(dep);
    if (!this.offcanvasService.hasOpenOffcanvas()) {
      this.offcanvasService.open(content, { position: 'bottom', scroll: true, backdrop: false });
    }
  }

  findDependencyInList(dep: IDependency): void {
    const index = this.dependencies?.findIndex(
      dependency =>
        dependency.library?.groupId === dep.library?.groupId &&
        dependency.library?.artifactId === dep.library?.artifactId &&
        dependency.library?.version === dep.library?.version &&
        dependency.library?.type === dep.library?.type
    );
    this.dependencies![index!] = dep;
  }

  setFileData(event: Event, field: string): void {
    this.uploadForm.get('fileName')!.setValue((event.target as HTMLInputElement).files![0].name);

    this.dataUtils.loadFileToForm(event, this.uploadForm, field, false).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(new EventWithContent<AlertError>('fossureApp.error', { message: err.message })),
    });
  }

  setAdditionalLibrariesData(event: Event, field: string): void {
    this.uploadForm.get('additionalLibrariesFileName')!.setValue((event.target as HTMLInputElement).files![0].name);

    this.dataUtils.loadFileToForm(event, this.uploadForm, field, false).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(new EventWithContent<AlertError>('fossureApp.error', { message: err.message })),
    });
  }

  upload(): void {
    if (this.uploadForm.get('url')!.value) {
      this.uploadByUrl();
    } else {
      if (this.project?.id) {
        const fileUpload = new File(
          this.uploadForm.get('fileName')!.value,
          this.uploadForm.get('fileContentType')!.value,
          this.uploadForm.get('file')!.value
        );
        const additionalLibrariesUpload = new File(
          this.uploadForm.get('additionalLibrariesFileName')!.value,
          this.uploadForm.get('additionalLibrariesContentType')!.value,
          this.uploadForm.get('additionalLibraries')!.value
        );

        this.projectService
          .upload(this.project.id, new Upload(fileUpload, additionalLibrariesUpload), {
            delete: this.uploadForm.get('deleteData')!.value,
          })
          .subscribe(() => {
            this.resetUploadFormSuccess();
          });
      }
    }
  }

  uploadByUrl(): void {
    if (this.project?.id) {
      this.projectService
        .uploadByUrl(
          this.project.id,
          {
            username: this.uploadForm.get('username')!.value,
            password: this.uploadForm.get('password')!.value,
          },
          {
            url: this.uploadForm.get('url')!.value,
            delete: this.uploadForm.get('deleteData')!.value,
          }
        )
        .subscribe({
          next: () => {
            this.resetUploadFormSuccess();
          },
          error: () => {
            this.resetUploadFormError();
          },
        });
    }
  }

  addLibrariesToProject(): void {
    if (this.project?.id) {
      this.projectService.addLibrary(this.project.id, this.addLibraryForm.get('libraries')!.value ?? []).subscribe(() => {
        this.modalService.dismissAll();
        this.loadPage(1);
      });
    }
  }

  hasErrors(library?: ILibrary | null): boolean | null {
    if (library) {
      return this.libraryService.hasErrors(library);
    }

    return null;
  }

  onLibraryTableChanges(): void {
    this.tableForm.valueChanges.subscribe(() => {
      this.saveLocalStorage();
    });
  }

  filterRisk(): void {
      this.isLoading = true;
      const licenseRiskFilter = this.createLicenseRiskFilter();

      this.dependencyService
        .query({
          'projectId.equals': this.project?.id,
          'artifactId.contains': this.searchForm.get('artifactId')?.value ?? null,
          'licensesShortIdentifier.contains': this.searchForm.get('licenses')?.value ?? null,
          'libraryRiskId.in': licenseRiskFilter?.join(',') ?? null,
          'errorLogMessage.contains': this.filterForm.get('licenseConflicts')!.value ? 'License Conflict' : null,
          'errorLogStatus.notEquals': this.filterForm.get('licenseConflicts')!.value ? 'CLOSED' : null,
          'libraryCreatedDate.greaterThanOrEqual': this.filterForm.get('newLibrariesSinceLastUpload')!.value
            ? this.project?.lastUpdatedDate?.format(DATE_FORMAT)
            : null,
          sort: this.sort(),
          page: 0,
          size: this.itemsPerPage,
        })
        .subscribe(
          (res: HttpResponse<IDependency[]>) => {
            this.isLoading = false;
            this.onLLPSuccess(res.body, res.headers, 1, true);
          },
          () => {
            this.isLoading = false;
            this.onError();
          }
        );
  }

  search(): void {
      this.isLoading = true;
      const riskFilter = this.createLicenseRiskFilter();

      this.dependencyService
        .query({
          'projectId.equals': this.project?.id,
          'artifactId.contains': this.searchForm.get('artifactId')?.value ?? null,
          'licensesShortIdentifier.contains': this.searchForm.get('licenses')?.value ?? null,
          'libraryRiskId.in': riskFilter?.join(',') ?? null,
          'errorLogMessage.contains': this.filterForm.get('licenseConflicts')!.value ? 'License Conflict' : null,
          'errorLogStatus.notEquals': this.filterForm.get('licenseConflicts')!.value ? 'CLOSED' : null,
          'libraryCreatedDate.greaterThanOrEqual': this.filterForm.get('newLibrariesSinceLastUpload')!.value
            ? this.project?.lastUpdatedDate?.format(DATE_FORMAT)
            : null,
          sort: this.sort(),
          page: 0,
          size: this.itemsPerPage,
        })
        .subscribe(
          (res: HttpResponse<IDependency[]>) => {
            this.isLoading = false;
            this.onLLPSuccess(res.body, res.headers, 1, true);
          },
          () => {
            this.isLoading = false;
            this.onError();
          }
        );
  }

  searchAddLibrary(): void {
    this.isLoadingAddSearch = true;

    this.libraryService
      .query({
        'groupId.contains': this.addLibraryForm.get('groupId')?.value ?? null,
        'artifactId.contains': this.addLibraryForm.get('artifactId')?.value ?? null,
        'version.contains': this.addLibraryForm.get('version')?.value ?? null,
        page: 0,
        size: this.itemsPerPage,
      })
      .subscribe(
        (res: HttpResponse<ILibrary[]>) => {
          this.isLoadingAddSearch = false;
          this.addSearchLibraries = res.body ?? [];
        },
        () => {
          this.isLoadingAddSearch = false;
        }
      );
  }

  addLibraryToAddLibraryForm(library: ILibrary): void {
    let libraries = this.addLibraryForm.get('libraries')!.value;
    if (!libraries) {
      libraries = [];
    }
    libraries.push(library);
    this.addLibraryForm.get('libraries')!.setValue(libraries);
  }

  removeLibraryFromAddLibraryForm(index: number): void {
    const libraries = this.addLibraryForm.get('libraries')!.value;
    if (libraries) {
      libraries.splice(index, 1);
    }
  }

  createNextProject(): void {
    if (this.project?.id) {
      this.projectService
        .createNextVersion(this.project.id, {
          version: this.nextProjectForm.get('version')!.value,
          delivered: this.nextProjectForm.get('delivered')!.value ?? null,
          copy: this.nextProjectForm.get('copy')!.value ?? null,
        })
        .subscribe((res: HttpResponse<IProject>) => {
          this.modalService.dismissAll();
          this.router.navigate(['/project', res.body?.id, 'view']);
        });
    }
  }

  delete(dep: IDependency): void {
    const modalRef = this.modalService.open(DependencyDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.dependency = dep;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadPage();
      }
    });
  }

  filter(): void {
      this.isLoading = true;
      const riskFilter = this.createLicenseRiskFilter();

      this.dependencyService
        .query({
          'projectId.equals': this.project?.id,
          'artifactId.contains': this.searchForm.get('artifactId')?.value ?? null,
          'licensesShortIdentifier.contains': this.searchForm.get('licenses')?.value ?? null,
          'libraryRiskId.in': riskFilter?.join(',') ?? null,
          'errorLogMessage.contains': this.filterForm.get('licenseConflicts')!.value ? 'License Conflict' : null,
          'errorLogStatus.notEquals': this.filterForm.get('licenseConflicts')!.value ? 'CLOSED' : null,
          'libraryCreatedDate.greaterThanOrEqual': this.filterForm.get('newLibrariesSinceLastUpload')!.value
            ? this.project?.lastUpdatedDate?.format(DATE_FORMAT)
            : null,
          sort: this.sort(),
          page: 0,
          size: this.itemsPerPage,
        })
        .subscribe(
          (res: HttpResponse<IDependency[]>) => {
            this.isLoading = false;
            this.onLLPSuccess(res.body, res.headers, 1, true);
          },
          () => {
            this.isLoading = false;
            this.onError();
          }
        );
  }

  saveLocalStorage(): void {
    const tableFormSettings = {
      groupId: this.tableForm.get(['groupId'])!.value,
      artifactId: this.tableForm.get(['artifactId'])!.value,
      version: this.tableForm.get(['version'])!.value,
      type: this.tableForm.get(['type'])!.value,
      licenses: this.tableForm.get(['licenses'])!.value,
      licenseToPublish: this.tableForm.get(['licenseToPublish'])!.value,
      risk: this.tableForm.get(['risk'])!.value,
      licenseUrl: this.tableForm.get(['licenseUrl'])!.value,
      licenseText: this.tableForm.get(['licenseText'])!.value,
      sourceCodeUrl: this.tableForm.get(['sourceCodeUrl'])!.value,
      copyright: this.tableForm.get(['copyright'])!.value,
      reviewed: this.tableForm.get(['reviewed'])!.value,
      hideForPublishing: this.tableForm.get(['hideForPublishing'])!.value,
      localComment: this.tableForm.get(['localComment'])!.value,
      errorLog: this.tableForm.get(['errorLog'])!.value,
      manuallyAdded: this.tableForm.get(['manuallyAdded'])!.value,
    };

    localStorage.setItem('project.libraryTableSettings', JSON.stringify(tableFormSettings));
  }

  loadLocalStorage(): void {
    const projectLibraryTableSettings = localStorage.getItem('project.libraryTableSettings');

    if (projectLibraryTableSettings) {
      const tableFormSettings = JSON.parse(projectLibraryTableSettings);

      this.tableForm.patchValue({
        groupId: tableFormSettings.groupId,
        artifactId: tableFormSettings.artifactId,
        version: tableFormSettings.version,
        type: tableFormSettings.type,
        licenses: tableFormSettings.licenses,
        licenseToPublish: tableFormSettings.licenseToPublish,
        risk: tableFormSettings.risk,
        licenseUrl: tableFormSettings.licenseUrl,
        licenseText: tableFormSettings.licenseText,
        sourceCodeUrl: tableFormSettings.sourceCodeUrl,
        copyright: tableFormSettings.copyright,
        reviewed: tableFormSettings.reviewed,
        hideForPublishing: tableFormSettings.hideForPublishing,
        localComment: tableFormSettings.localComment,
        errorLog: tableFormSettings.errorLog,
        manuallyAdded: tableFormSettings.manuallyAdded,
      });
    }
  }

  dismiss(reason?: string): void {
    this.offcanvasService.dismiss(reason);
  }

  save(): void {
    this.isSaving = true;
    const dep = this.createFromForm();
    this.subscribeToSaveResponse(this.dependencyService.partialUpdate(dep));
  }

  navTabChange(event: any): void {
    if (event.nextId === 'Statistics' && !this.statistic && this.project?.id) {
      this.projectService.statistic(this.project.id).subscribe((res: HttpResponse<IProjectStatistic>) => {
        this.statistic = res.body;
      });
    }
  }

  protected resetUploadFormSuccess(): void {
    this.modalService.dismissAll();
    this.uploadForm.get('url')!.setValue(null);
    this.uploadForm.get('file')!.setValue(null);
    this.uploadForm.get('fileContentType')!.setValue(null);
    this.uploadForm.get('fileName')!.setValue(null);
    this.uploadForm.get('additionalLibraries')!.setValue(null);
    this.uploadForm.get('additionalLibrariesContentType')!.setValue(null);
    this.uploadForm.get('additionalLibrariesFileName')!.setValue(null);
    this.uploadForm.get('deleteData')!.setValue(true);
    this.loadPage(1);
  }

  protected resetUploadFormError(): void {
    this.modalService.dismissAll();
    this.uploadForm.get('url')!.setValue(null);
    this.uploadForm.get('username')!.setValue(null);
    this.uploadForm.get('password')!.setValue(null);
    this.uploadForm.get('file')!.setValue(null);
    this.uploadForm.get('fileContentType')!.setValue(null);
    this.uploadForm.get('fileName')!.setValue(null);
    this.uploadForm.get('additionalLibraries')!.setValue(null);
    this.uploadForm.get('additionalLibrariesContentType')!.setValue(null);
    this.uploadForm.get('additionalLibrariesFileName')!.setValue(null);
    this.uploadForm.get('deleteData')!.setValue(true);
  }

  protected updateForm(dep: IDependency): void {
    this.dependencyForm.patchValue({
      hide: dep.hideForPublishing,
      addedManually: dep.addedManually,
      localComment: dep.comment,
    });
  }

  protected createFromForm(): IDependency {
    return {
      ...new Dependency(),
      id: this.dependency?.id,
      hideForPublishing: this.dependencyForm.get(['hide'])!.value,
      comment: this.dependencyForm.get(['localComment'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDependency>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: (lib: HttpResponse<IDependency>) => this.onSaveSuccess(lib.body!),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected onSaveSuccess(dep: IDependency): void {
    this.saved = true;
    this.findDependencyInList(dep);
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected createLicenseRiskFilter(): string[] | null {
    const filter = Object.keys(this.riskForm.controls).filter(key => this.riskForm.get(key)?.value === true);

    if (filter.length === 7) {
      return null;
    }

    return filter;
  }

  protected handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get('page');
      const pageNumber = +(page ?? 1);
      const sort = (params.get(SORT) ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === ASC;

      const artifactId = params.get('artifactId');
      const licensesShortIdentifier = params.get('licenses');

      const licenseConflicts = params.get('licenseConflicts');
      const newLibraries = params.get('newLibraries');

      this.searchForm.get('artifactId')?.setValue(artifactId);
      this.searchForm.get('licenses')?.setValue(licensesShortIdentifier);

      if (params.get('libraryRiskId')) {
        const libraryRiskId = params.get('libraryRiskId')?.split(',');

        Object.keys(this.riskForm.controls).forEach(key => {
          if (libraryRiskId?.includes(key)) {
            this.riskForm.get(key)?.setValue(true);
          } else {
            this.riskForm.get(key)?.setValue(false);
          }
        });
      }

      if (licenseConflicts === 'true') {
        this.filterForm.get('licenseConflicts')!.setValue(true);
      } else {
        this.filterForm.get('licenseConflicts')!.setValue(false);
      }
      if (newLibraries === 'true') {
        this.filterForm.get('newLibrariesSinceLastUpload')!.setValue(true);
      } else {
        this.filterForm.get('newLibrariesSinceLastUpload')!.setValue(false);
      }

      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    });
  }

  protected sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? ASC : DESC)];
    if (this.predicate !== 'library.artifactId') {
      result.push('library.artifactId');
    }
    return result;
  }

  protected onProjectSuccess(data: IProject | null): void {
    this.project = data ?? null;
    this.uploadStateType = this.project?.uploadState ?? UploadState.OK;
  }

  protected onLLPSuccess(data: IDependency[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;

    const riskFilter = this.createLicenseRiskFilter();

    if (navigate) {
      this.router.navigate([`/project/${this.project!.id!}/view`], {
        queryParams: {
          artifactId: this.searchForm.get('artifactId')?.value ?? null,
          licenses: this.searchForm.get('licenses')?.value ?? null,
          libraryRiskId: riskFilter?.join(',') ?? null,
          licenseConflicts: this.filterForm.get('licenseConflicts')!.value ?? null,
          newLibraries: this.filterForm.get('newLibrariesSinceLastUpload')!.value ?? null,
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? ASC : DESC),
        },
      });
    }

    this.dependencies = data;
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
