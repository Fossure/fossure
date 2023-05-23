import { Component, Input, OnInit } from '@angular/core';

import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { ProjectService } from '../../../entities/project/service/project.service';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { IProject } from '../../../entities/project/project.model';
import { ActivatedRoute } from '@angular/router';
import { IDifferenceView } from './difference-view.model';
import { ASC } from '../../../config/pagination.constants';
import { ILibrary } from '../../../entities/library/library.model';
import { LibraryService } from '../../../entities/library/service/library.service';

@Component({
  selector: 'jhi-difference-view',
  templateUrl: './difference-view.component.html',
})
export class DifferenceViewComponent implements OnInit {
  @Input()
  showProjectSelection = true;

  differenceView?: IDifferenceView | null;
  noDifferenceViewPossible = false;

  isLoading = false;
  isCollapsed = true;
  projectsSharedCollection: IProject[] = [];

  projectComparisonForm = this.fb.group({
    firstProject: [null, [Validators.required]],
    secondProject: [null, [Validators.required]],
  });

  optionsForm = this.fb.group({
    showLeft: true,
    showCenter: true,
    showRight: true,
    onlyNewLibraries: false,
    showLicense: true,
    showRisk: false,
  });

  constructor(
    protected projectService: ProjectService,
    protected fb: UntypedFormBuilder,
    protected activatedRoute: ActivatedRoute,
    protected libraryService: LibraryService
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(() => {
      if (this.showProjectSelection) {
        this.loadRelationshipsOptions();
      }
    });
  }

  compare(): void {
    const firstProject = this.projectComparisonForm.get('firstProject')!.value as unknown as IProject;
    const secondProject = this.projectComparisonForm.get('secondProject')!.value as unknown as IProject;

    this.isLoading = true;

    this.projectService
      .compareProjects({
        firstProjectId: firstProject.id,
        secondProjectId: secondProject.id,
      })
      .subscribe({
        next: (res: HttpResponse<IDifferenceView>) => {
          this.differenceView = res.body;
          this.isLoading = false;
        },
        error: () => (this.isLoading = false),
      });
  }

  compareWithParameters(firstProjectId: number | undefined | null, secondProjectId: number | undefined | null): void {
    this.isLoading = true;

    if (firstProjectId && secondProjectId) {
      this.projectService
        .compareProjects({
          firstProjectId,
          secondProjectId,
        })
        .subscribe({
          next: (res: HttpResponse<IDifferenceView>) => {
            this.differenceView = res.body;

            this.loadRelationshipsOptionsAndInitProjectComparisonForm(firstProjectId, secondProjectId);
          },
          error: () => (this.isLoading = false),
        });
    } else {
      this.noDifferenceViewPossible = true;
      this.isLoading = false;
    }
  }

  trackProjectById(index: number, item: IProject): number {
    return item.id!;
  }

  getSingleProject(option: IProject, selectedVal?: IProject): IProject {
    if (selectedVal) {
      if (option.id === selectedVal.id) {
        return selectedVal;
      }
    }
    return option;
  }

  isNew(newLibraryList: ILibrary[] | undefined | null, library: ILibrary): boolean {
    return !!newLibraryList?.some(e => e.id === library.id);
  }

  protected loadRelationshipsOptions(): void {
    this.projectService.count().subscribe((count: HttpResponse<number>) => {
      this.projectService
        .query({
          page: 0,
          size: count.body,
          sort: ['identifier' + ',' + ASC],
        })
        .pipe(map((res: HttpResponse<IProject[]>) => res.body ?? []))
        .subscribe((projects: IProject[]) => {
          this.projectsSharedCollection = projects;
        });
    });
  }

  protected loadRelationshipsOptionsAndInitProjectComparisonForm(
    firstProjectId: number | undefined | null,
    secondProjectId: number | undefined | null
  ): void {
    this.projectService.count().subscribe((count: HttpResponse<number>) => {
      this.projectService
        .query({
          page: 0,
          size: count.body,
          sort: ['identifier' + ',' + ASC],
        })
        .pipe(map((res: HttpResponse<IProject[]>) => res.body ?? []))
        .subscribe((projects: IProject[]) => {
          this.projectsSharedCollection = projects;

          const firstProject = this.projectsSharedCollection.find(e => e.id === firstProjectId);
          if (firstProject) {
            this.projectComparisonForm.get('firstProject')?.setValue(firstProject);
          }

          const secondProject = this.projectsSharedCollection.find(e => e.id === secondProjectId);
          if (secondProject) {
            this.projectComparisonForm.get('secondProject')?.setValue(secondProject);
          }
          this.isLoading = false;
        });
    });
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    // Api for inheritance.
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }
}
