import { HttpResponse } from '@angular/common/http';
import { Component, Input, TemplateRef } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgbOffcanvas } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { EventManager } from '../../../core/util/event-manager.service';
import { IDependency, Dependency } from '../../dependency/dependency.model';
import { DependencyService } from '../../dependency/service/dependency.service';
import { ProjectService } from '../service/project.service';

@Component({
  selector: 'jhi-project-update-library',
  templateUrl: './project-update-library.component.html',
})
export class ProjectUpdateLibraryComponent {
  closeResult = '';
  isSaving = false;
  saved = false;

  @Input()
  set dependency(dependency: IDependency | undefined) {
    this.saved = false;
    this._dependency = dependency;
    this.updateForm(this._dependency!);
  }

  get dependency(): IDependency | undefined {
    return this._dependency;
  }

  dependencyForm = this.fb.group({
    hide: [],
    addedManually: [],
    comment: [],
  });

  private _dependency?: IDependency;

  constructor(
    protected offcanvasService: NgbOffcanvas,
    protected eventManager: EventManager,
    protected projectService: ProjectService,
    protected activatedRoute: ActivatedRoute,
    protected fb: UntypedFormBuilder,
    protected dependencyService: DependencyService
  ) {}

  open(content: TemplateRef<any>): void {
    if (!this.offcanvasService.hasOpenOffcanvas()) {
      this.offcanvasService.open(content, { position: 'bottom', scroll: true });
    }
  }

  dismiss(reason?: string): void {
    this.offcanvasService.dismiss(reason);
  }

  save(): void {
    this.isSaving = true;
    const dependency = this.createFromForm();
    this.subscribeToSaveResponse(this.dependencyService.partialUpdate(dependency));
  }

  protected updateForm(dependency: IDependency): void {
    this.dependencyForm.patchValue({
      hide: dependency.hideForPublishing,
      addedManually: dependency.addedManually,
    });
  }

  protected createFromForm(): IDependency {
    return {
      ...new Dependency(),
      id: this.dependency?.id,
      hideForPublishing: this.dependencyForm.get(['hide'])!.value,
      addedManually: this.dependencyForm.get(['addedManually'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDependency>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected onSaveSuccess(): void {
    this.saved = true;
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }
}
