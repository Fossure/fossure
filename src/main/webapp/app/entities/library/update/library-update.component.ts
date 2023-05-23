import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import { DataUtils, FileLoadError } from 'app/core/util/data-util.service';
import { EventManager, EventWithContent } from 'app/core/util/event-manager.service';
import { LibraryType } from 'app/entities/enumerations/library-type.model';
import { LinkType } from 'app/entities/enumerations/link-type.model';
import { LibraryService } from 'app/entities/library/service/library.service';
import { ILicensePerLibrary } from 'app/entities/license-per-library/license-per-library.model';
import { ILicense } from 'app/entities/license/license.model';
import { LicenseService } from 'app/entities/license/service/license.service';
import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/user.service';
import { AlertError } from 'app/shared/alert/alert-error.model';
import { ILibrary, Library } from '../library.model';

@Component({
  selector: 'jhi-library-update',
  templateUrl: './library-update.component.html',
})
export class LibraryUpdateComponent implements OnInit {
  isSaving = false;
  libraryTypeValues = Object.keys(LibraryType);

  usersSharedCollection: IUser[] = [];
  licensesSharedCollection: ILicense[] = [];

  linkTypes = Object.keys(LinkType);

  editForm = this.fb.group({
    id: [],
    namespace: [],
    name: [null, [Validators.required]],
    version: [null, [Validators.required]],
    type: [],
    originalLicense: [null, [Validators.maxLength(2048)]],
    licenseUrl: [null, [Validators.maxLength(2048)]],
    licenseText: [],
    sourceCodeUrl: [null, [Validators.maxLength(2048)]],
    pUrl: [null, [Validators.maxLength(2048)]],
    copyright: [null, [Validators.maxLength(16384)]],
    compliance: [],
    complianceComment: [null, [Validators.maxLength(4096)]],
    comment: [null, [Validators.maxLength(4096)]],
    reviewed: [],
    reviewedDeepScan: [],
    lastReviewedDate: [],
    lastReviewedDeepScanDate: [],
    createdDate: [],
    hideForPublishing: [],
    md5: [null, [Validators.maxLength(32)]],
    sha1: [null, [Validators.maxLength(40)]],
    lastReviewedBy: [],
    lastReviewedDeepScanBy: [],
    errorLogs: [],
    licenseToPublishes: [],
    licenseOfFiles: [],
    linkType: [],
    linkedLicenses: this.fb.array([
      this.fb.group({
        license: null,
        linkType: null,
      }),
    ]),
    fossology: [],
  });

  constructor(
    protected dataUtils: DataUtils,
    protected eventManager: EventManager,
    protected libraryService: LibraryService,
    protected userService: UserService,
    protected licenseService: LicenseService,
    protected activatedRoute: ActivatedRoute,
    protected fb: UntypedFormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ library }) => {
      this.updateForm(library);

      this.loadRelationshipsOptions();
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  setFileData(event: Event, field: string, isImage: boolean): void {
    this.dataUtils.loadFileToForm(event, this.editForm, field, isImage).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(new EventWithContent<AlertError>('fossureApp.error', { message: err.message })),
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const library = this.createFromForm();
    if (library.id !== undefined) {
      this.subscribeToSaveResponse(this.libraryService.update(library));
    } else {
      this.subscribeToSaveResponse(this.libraryService.create(library));
    }
  }

  trackUserById(index: number, item: IUser): number {
    return item.id!;
  }

  trackLicenseById(index: number, item: ILicense): number {
    return item.id!;
  }

  getSelectedLicense(option: ILicense, selectedVals?: ILicense[]): ILicense {
    if (selectedVals) {
      for (const selectedVal of selectedVals) {
        if (option.id === selectedVal.id) {
          return selectedVal;
        }
      }
    }
    return option;
  }

  getSingleLicense(option: ILicense, selectedVal?: ILicense): ILicense {
    if (selectedVal) {
      if (option.id === selectedVal.id) {
        return selectedVal;
      }
    }
    return option;
  }

  getLinkedLicenses(): UntypedFormArray {
    return this.editForm.get('linkedLicenses') as UntypedFormArray;
  }

  getLicense(i: number): ILicense {
    return this.getLinkedLicenses().at(i).get('license')?.value as ILicense;
  }

  getLinkType(i: number): string {
    return this.getLinkedLicenses().at(i).get('linkType')?.value as string;
  }

  lastLicenseNotNull(): boolean {
    const licenses = this.getLinkedLicenses();
    return !!licenses.at(licenses.length - 1).get('license')?.value;
  }

  newLinkedLicense(): UntypedFormGroup {
    return this.fb.group({
      license: null,
      linkType: null,
    });
  }

  newLinkedLicenseParam(licenseValue: ILicense | null, type: LinkType | null): UntypedFormGroup {
    return this.fb.group({
      license: licenseValue,
      linkType: type,
    });
  }

  addLicense(): void {
    const licenses = this.getLinkedLicenses();
    if (licenses.length > 0) {
      licenses.at(licenses.length - 1).patchValue({
        linkType: LinkType.AND,
      });
    }
    licenses.push(this.newLinkedLicense());
  }

  removeLicense(i: number): void {
    const licenses = this.getLinkedLicenses();
    licenses.removeAt(i);

    if (licenses.length > 0) {
      licenses.at(licenses.length - 1).patchValue({
        linkType: null,
      });
    }
  }

  /* reviewedAutocomplete(event: Event): void {
    if(event.target.checked) {
      this.editForm.controls['lastReviewedDate'].setValue(dayjs().format(DATE_FORMAT);
    }
  }*/

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ILibrary>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(library: ILibrary): void {
    this.editForm.patchValue({
      id: library.id,
      namespace: library.namespace,
      name: library.name,
      version: library.version,
      type: library.type,
      originalLicense: library.originalLicense,
      licenseUrl: library.licenseUrl,
      licenseText: library.licenseText,
      sourceCodeUrl: library.sourceCodeUrl,
      pUrl: library.pUrl,
      copyright: library.copyright,
      compliance: library.compliance,
      complianceComment: library.complianceComment,
      comment: library.comment,
      reviewed: library.reviewed,
      reviewedDeepScan: library.reviewedDeepScan,
      lastReviewedDate: library.lastReviewedDate,
      lastReviewedDeepScanDate: library.lastReviewedDeepScanDate,
      createdDate: library.createdDate,
      hideForPublishing: library.hideForPublishing,
      md5: library.md5,
      sha1: library.sha1,
      lastReviewedBy: library.lastReviewedBy,
      lastReviewedDeepScanBy: library.lastReviewedDeepScanBy,
      errorLogs: library.errorLogs,
      licenseToPublishes: library.licenseToPublishes,
      licenseOfFiles: library.licenseOfFiles,
      linkedLicenses: library.licenses,
      fossology: library.fossology,
    });

    this.updateLinkedLicensesForm(library.licenses ?? null);

    this.usersSharedCollection = this.userService.addUserToCollectionIfMissing(
      this.usersSharedCollection,
      library.lastReviewedBy,
      library.lastReviewedDeepScanBy
    );
    this.licensesSharedCollection = this.licenseService.addLicenseToCollectionIfMissing(
      this.licensesSharedCollection,
      ...(library.licenseToPublishes ?? []),
      ...(library.licenseOfFiles ?? [])
    );
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(
        map((users: IUser[]) =>
          this.userService.addUserToCollectionIfMissing(
            users,
            this.editForm.get('lastReviewedBy')!.value,
            this.editForm.get('lastReviewedDeepScanBy')!.value
          )
        )
      )
      .subscribe((users: IUser[]) => (this.usersSharedCollection = users));

    this.licenseService
      .allLicenseNames()
      .pipe(map((res: HttpResponse<ILicense[]>) => res.body ?? []))
      .pipe(
        map((licenses: ILicense[]) =>
          this.licenseService.addLicenseToCollectionIfMissing(
            licenses,
            ...(this.editForm.get('licenseToPublishes')!.value ?? []),
            ...(this.editForm.get('licenseOfFiles')!.value ?? [])
          )
        )
      )
      .subscribe((licenses: ILicense[]) => (this.licensesSharedCollection = licenses));
  }

  protected createFromForm(): ILibrary {
    return {
      ...new Library(),
      id: this.editForm.get(['id'])!.value,
      namespace: this.editForm.get(['namespace'])!.value,
      name: this.editForm.get(['name'])!.value,
      version: this.editForm.get(['version'])!.value,
      type: this.editForm.get(['type'])!.value,
      originalLicense: this.editForm.get(['originalLicense'])!.value,
      licenseUrl: this.editForm.get(['licenseUrl'])!.value,
      licenseText: this.editForm.get(['licenseText'])!.value,
      sourceCodeUrl: this.editForm.get(['sourceCodeUrl'])!.value,
      pUrl: this.editForm.get(['pUrl'])!.value,
      copyright: this.editForm.get(['copyright'])!.value,
      compliance: this.editForm.get(['compliance'])!.value,
      complianceComment: this.editForm.get(['complianceComment'])!.value,
      comment: this.editForm.get(['comment'])!.value,
      reviewed: this.editForm.get(['reviewed'])!.value,
      reviewedDeepScan: this.editForm.get(['reviewedDeepScan'])!.value,
      lastReviewedDate: this.editForm.get(['lastReviewedDate'])!.value,
      lastReviewedDeepScanDate: this.editForm.get(['lastReviewedDeepScanDate'])!.value,
      createdDate: this.editForm.get(['createdDate'])!.value,
      hideForPublishing: this.editForm.get(['hideForPublishing'])!.value,
      md5: this.editForm.get(['md5'])!.value,
      sha1: this.editForm.get(['sha1'])!.value,
      lastReviewedBy: this.editForm.get(['lastReviewedBy'])!.value,
      lastReviewedDeepScanBy: this.editForm.get(['lastReviewedDeepScanBy'])!.value,
      errorLogs: this.editForm.get(['errorLogs'])!.value,
      licenseToPublishes: this.editForm.get(['licenseToPublishes'])!.value,
      licenseOfFiles: this.editForm.get(['licenseOfFiles'])!.value,
      licenses: this.buildILicensePerLibrary(),
      fossology: this.editForm.get(['fossology'])!.value,
    };
  }

  protected updateLinkedLicensesForm(licensesPerLibrary: ILicensePerLibrary[] | null): void {
    const licenses = this.getLinkedLicenses();
    licenses.clear();
    if (licensesPerLibrary) {
      licensesPerLibrary.forEach((value: ILicensePerLibrary) => {
        licenses.push(this.newLinkedLicenseParam(value.license ?? null, value.linkType ?? null));
      });
    }
  }

  protected buildILicensePerLibrary(): ILicensePerLibrary[] {
    const linkedLicenses: ILicensePerLibrary[] = [];
    let orderIdCounter = 0;

    this.getLinkedLicenses().controls.forEach(function (value: any) {
      const tmp: ILicensePerLibrary = {
        linkType: value.get('linkType')!.value,
        license: value.get('license')!.value,
        orderId: orderIdCounter,
      };
      linkedLicenses.push(tmp);
      orderIdCounter++;
    });

    return linkedLicenses;
  }
}
