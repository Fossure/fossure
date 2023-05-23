import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IDependency, getDependencyIdentifier } from '../dependency.model';

export type EntityResponseType = HttpResponse<IDependency>;
export type EntityArrayResponseType = HttpResponse<IDependency[]>;

@Injectable({ providedIn: 'root' })
export class DependencyService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/v1/dependencies');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(dependency: IDependency): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(dependency);
    return this.http
      .post<IDependency>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(dependency: IDependency): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(dependency);
    return this.http
      .put<IDependency>(`${this.resourceUrl}/${getDependencyIdentifier(dependency) as number}`, copy, {
        observe: 'response',
      })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  partialUpdate(dependency: IDependency): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(dependency);
    return this.http
      .patch<IDependency>(`${this.resourceUrl}/${getDependencyIdentifier(dependency) as number}`, copy, {
        observe: 'response',
        headers: new HttpHeaders({ 'Content-Type': 'application/merge-patch+json' }),
      })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IDependency>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IDependency[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  addDependencyToCollectionIfMissing(
    dependencyCollection: IDependency[],
    ...dependenciesToCheck: (IDependency | null | undefined)[]
  ): IDependency[] {
    const dependencies: IDependency[] = dependenciesToCheck.filter(isPresent);
    if (dependencies.length > 0) {
      const dependencyCollectionIdentifiers = dependencyCollection.map(
        dependencyItem => getDependencyIdentifier(dependencyItem)!
      );
      const dependenciesToAdd = dependencies.filter(dependencyItem => {
        const dependencyIdentifier = getDependencyIdentifier(dependencyItem);
        if (dependencyIdentifier == null || dependencyCollectionIdentifiers.includes(dependencyIdentifier)) {
          return false;
        }
        dependencyCollectionIdentifiers.push(dependencyIdentifier);
        return true;
      });
      return [...dependenciesToAdd, ...dependencyCollection];
    }
    return dependencyCollection;
  }

  protected convertDateFromClient(dependency: IDependency): IDependency {
    return Object.assign({}, dependency, {
      addedDate: dependency.addedDate?.isValid() ? dependency.addedDate.format(DATE_FORMAT) : undefined,
    });
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.addedDate = res.body.addedDate ? dayjs(res.body.addedDate) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((dependency: IDependency) => {
        dependency.addedDate = dependency.addedDate ? dayjs(dependency.addedDate) : undefined;
      });
    }
    return res;
  }
}
