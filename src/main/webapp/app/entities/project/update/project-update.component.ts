import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import dayjs from 'dayjs/esm';

import { DataUtils, FileLoadError } from 'app/core/util/data-util.service';
import { EventManager, EventWithContent } from 'app/core/util/event-manager.service';
import { UploadState } from 'app/entities/enumerations/upload-state.model';
import { AlertError } from 'app/shared/alert/alert-error.model';
import { IProject, Project } from '../project.model';
import { ProjectService } from '../service/project.service';

@Component({
  selector: 'jhi-project-update',
  templateUrl: './project-update.component.html',
})
export class ProjectUpdateComponent implements OnInit {
  projectsSharedCollection: IProject[] = [];

  isSaving = false;
  uploadStateValues = Object.keys(UploadState);

  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    label: [null, [Validators.required]],
    version: [null, [Validators.required]],
    createdDate: [],
    lastUpdatedDate: [],
    uploadState: [],
    disclaimer: [],
    delivered: [],
    deliveredDate: [],
    contact: [null, [Validators.maxLength(2048)]],
    comment: [null, [Validators.maxLength(4096)]],
    previousProject: [],
    uploadFilter: [null, [Validators.maxLength(2048)]],
  });

  constructor(
    protected dataUtils: DataUtils,
    protected eventManager: EventManager,
    protected projectService: ProjectService,
    protected activatedRoute: ActivatedRoute,
    protected fb: UntypedFormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ project }) => {
      if (project.id === undefined) {
        const today = dayjs().startOf('day');
        project.deliveredDate = today;
      }

      this.updateForm(project);
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
    const project = this.createFromForm();
    if (project.id !== undefined) {
      this.subscribeToSaveResponse(this.projectService.update(project));
    } else {
      this.subscribeToSaveResponse(this.projectService.create(project));
    }
  }

  trackProjectById(index: number, item: IProject): number {
    return item.id!;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProject>>): void {
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

  protected updateForm(project: IProject): void {
    this.projectService
      .count({
        'label.equals': project.label,
        'version.notEquals': project.version,
      })
      .subscribe((count: HttpResponse<number>) => {
        this.projectService
          .query({
            'label.equals': project.label,
            'version.notEquals': project.version,
            page: 0,
            size: count.body,
          })
          .subscribe((res: HttpResponse<IProject[]>) => {
            this.projectsSharedCollection = res.body ?? [];
            this.editForm.patchValue({
              id: project.id,
              name: project.name,
              label: project.label,
              version: project.version,
              createdDate: project.createdDate,
              lastUpdatedDate: project.lastUpdatedDate,
              uploadState: project.uploadState,
              delivered: project.delivered,
              deliveredDate: project.deliveredDate,
              contact: project.contact,
              comment: project.comment,
              disclaimer: project.disclaimer,
              previousProject: this.projectsSharedCollection.find((ele: IProject) => ele.id === project.previousProject?.id) ?? null,
              uploadFilter: project.uploadFilter,
            });
          });
      });
  }

  protected createFromForm(): IProject {
    return {
      ...new Project(),
      id: this.editForm.get(['id'])!.value,
      name: this.editForm.get(['name'])!.value,
      label: this.editForm.get(['label'])!.value,
      version: this.editForm.get(['version'])!.value,
      createdDate: this.editForm.get(['createdDate'])!.value,
      lastUpdatedDate: this.editForm.get(['lastUpdatedDate'])!.value,
      uploadState: this.editForm.get(['uploadState'])!.value,
      disclaimer: this.editForm.get(['disclaimer'])!.value,
      delivered: this.editForm.get(['delivered'])!.value,
      deliveredDate: this.editForm.get(['deliveredDate'])!.value
        ? dayjs(this.editForm.get(['deliveredDate'])!.value, DATE_TIME_FORMAT)
        : undefined,
      contact: this.editForm.get(['contact'])!.value,
      comment: this.editForm.get(['comment'])!.value,
      previousProject: this.editForm.get(['previousProject'])?.value as IProject | undefined,
      uploadFilter: this.editForm.get(['uploadFilter'])!.value,
    };
  }
}
