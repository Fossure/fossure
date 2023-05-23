import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { isPresent } from 'app/core/util/operators';
import { IProject, getProjectIdentifier } from '../project.model';

import { IFile } from 'app/core/file/file.model';
import { IUpload } from 'app/core/upload/upload.model';
import { ILibrary } from 'app/entities/library/library.model';
import { IBasicAuth } from 'app/shared/auth/basic-auth.model';
import { ICountOccurrences } from 'app/shared/statistics/count-occurrences.model';
import { IProjectOverview } from 'app/shared/statistics/project-overview/project-overview.model';
import { IDifferenceView } from '../../../shared/modals/difference-view-modal/difference-view.model';
import { IProjectStatistic } from '../../../shared/statistics/project-overview/project-statistic.model';

export type EntityResponseType = HttpResponse<IProject>;
export type EntityArrayResponseType = HttpResponse<IProject[]>;

@Injectable({ providedIn: 'root' })
export class ProjectService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/v1/projects');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(project: IProject): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(project);
    return this.http
      .post<IProject>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(project: IProject): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(project);
    return this.http
      .put<IProject>(`${this.resourceUrl}/${getProjectIdentifier(project) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  partialUpdate(project: IProject): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(project);
    return this.http
      .patch<IProject>(`${this.resourceUrl}/${getProjectIdentifier(project) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IProject>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IProject[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  count(req?: any): Observable<HttpResponse<number>> {
    const options = createRequestOption(req);
    return this.http.get<number>(`${this.resourceUrl}/count`, { params: options, observe: 'response' });
  }

  oss(id: number, req?: any): Observable<HttpResponse<IFile>> {
    const options = createRequestOption(req);
    return this.http.get<IFile>(`${this.resourceUrl}/${id}/oss`, { params: options, observe: 'response' });
  }

  archive(id: number, req?: any): Observable<HttpResponse<IFile>> {
    const options = createRequestOption(req);
    return this.http.get<IFile>(`${this.resourceUrl}/${id}/create-archive`, { params: options, observe: 'response' });
  }

  zip(id: number): Observable<HttpResponse<IFile>> {
    return this.http.get<IFile>(`${this.resourceUrl}/${id}/zip`, { observe: 'response' });
  }

  overview(id: number): Observable<HttpResponse<IProjectOverview>> {
    return this.http.get<IProjectOverview>(`${this.resourceUrl}/${id}/overview`, { observe: 'response' });
  }

  statistic(id: number): Observable<HttpResponse<IProjectStatistic>> {
    return this.http.get<IProjectStatistic>(`${this.resourceUrl}/${id}/statistic`, { observe: 'response' });
  }

  risk(id: number): Observable<HttpResponse<ICountOccurrences[]>> {
    return this.http.get<ICountOccurrences[]>(`${this.resourceUrl}/${id}/risk`, { observe: 'response' });
  }

  licenses(id: number): Observable<HttpResponse<ICountOccurrences[]>> {
    return this.http.get<ICountOccurrences[]>(`${this.resourceUrl}/${id}/licenses`, { observe: 'response' });
  }

  createNextVersion(id: number, req?: any): Observable<EntityResponseType> {
    const options = createRequestOption(req);
    return this.http
      .post<IProject>(`${this.resourceUrl}/${id}/create-next-version`, null, { params: options, observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  upload(id: number, upload: IUpload, req?: any): Observable<HttpResponse<{}>> {
    const options = createRequestOption(req);
    return this.http.post<IUpload>(`${this.resourceUrl}/${id}/upload`, upload, { params: options, observe: 'response' });
  }

  uploadByUrl(id: number, credentials: IBasicAuth, req?: any): Observable<HttpResponse<{}>> {
    const options = createRequestOption(req);
    return this.http.post(`${this.resourceUrl}/${id}/upload-by-url`, credentials, { params: options, observe: 'response' });
  }

  addLibrary(id: number, libraries: ILibrary[]): Observable<HttpResponse<{}>> {
    return this.http.post<IProject>(`${this.resourceUrl}/${id}/add-libraries`, libraries, { observe: 'response' });
  }

  compareProjects(req?: any): Observable<HttpResponse<IDifferenceView>> {
    const options = createRequestOption(req);
    return this.http.get<IDifferenceView>(`${this.resourceUrl}/compare`, { params: options, observe: 'response' });
  }

  addProjectToCollectionIfMissing(projectCollection: IProject[], ...projectsToCheck: (IProject | null | undefined)[]): IProject[] {
    const projects: IProject[] = projectsToCheck.filter(isPresent);
    if (projects.length > 0) {
      const projectCollectionIdentifiers = projectCollection.map(projectItem => getProjectIdentifier(projectItem)!);
      const projectsToAdd = projects.filter(projectItem => {
        const projectIdentifier = getProjectIdentifier(projectItem);
        if (projectIdentifier == null || projectCollectionIdentifiers.includes(projectIdentifier)) {
          return false;
        }
        projectCollectionIdentifiers.push(projectIdentifier);
        return true;
      });
      return [...projectsToAdd, ...projectCollection];
    }
    return projectCollection;
  }

  protected convertDateFromClient(project: IProject): IProject {
    return Object.assign({}, project, {
      createdDate: project.createdDate?.isValid() ? project.createdDate.format(DATE_FORMAT) : undefined,
      lastUpdatedDate: project.lastUpdatedDate?.isValid() ? project.lastUpdatedDate.format(DATE_FORMAT) : undefined,
      deliveredDate: project.deliveredDate?.isValid() ? project.deliveredDate.toJSON() : undefined,
    });
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdDate = res.body.createdDate ? dayjs(res.body.createdDate) : undefined;
      res.body.lastUpdatedDate = res.body.lastUpdatedDate ? dayjs(res.body.lastUpdatedDate) : undefined;
      res.body.deliveredDate = res.body.deliveredDate ? dayjs(res.body.deliveredDate) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((project: IProject) => {
        project.createdDate = project.createdDate ? dayjs(project.createdDate) : undefined;
        project.lastUpdatedDate = project.lastUpdatedDate ? dayjs(project.lastUpdatedDate) : undefined;
        project.deliveredDate = project.deliveredDate ? dayjs(project.deliveredDate) : undefined;
      });
    }
    return res;
  }
}
