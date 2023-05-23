import { HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IProject, Project } from '../project.model';
import { ProjectService } from '../service/project.service';

@Injectable({ providedIn: 'root' })
export class ProjectRoutingResolveService implements Resolve<IProject> {
  constructor(protected service: ProjectService, protected router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IProject> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((project: HttpResponse<Project>) => {
          if (project.body) {
            return of(project.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Project());
  }
}
