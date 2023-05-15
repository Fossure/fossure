import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ICopyright } from 'app/core/copyright/copyright.model';
import { IFile } from 'app/core/file/file.model';
import { createRequestOption } from 'app/core/request/request-util';
import { isPresent } from 'app/core/util/operators';
import { LogStatus } from 'app/entities/enumerations/log-status.model';
import { ILibraryErrorLog } from 'app/entities/library-error-log/library-error-log.model';
import { IFossology } from '../../fossology/fossology.model';
import { ILibrary, getLibraryIdentifier } from '../library.model';

export type EntityResponseType = HttpResponse<ILibrary>;
export type EntityArrayResponseType = HttpResponse<ILibrary[]>;

@Injectable({ providedIn: 'root' })
export class LibraryService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/v1/libraries');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(library: ILibrary): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(library);
    return this.http
      .post<ILibrary>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(library: ILibrary): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(library);
    return this.http
      .put<ILibrary>(`${this.resourceUrl}/${getLibraryIdentifier(library) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  partialUpdate(library: ILibrary): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(library);
    return this.http
      .patch<ILibrary>(`${this.resourceUrl}/${getLibraryIdentifier(library) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ILibrary>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ILibrary[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  count(req?: any): Observable<HttpResponse<number>> {
    const options = createRequestOption(req);
    return this.http.get<number>(`${this.resourceUrl}/count`, { params: options, observe: 'response' });
  }

  export(req?: any): Observable<HttpResponse<IFile>> {
    const options = createRequestOption(req);
    return this.http.get<IFile>(`${this.resourceUrl}/export`, { params: options, observe: 'response' });
  }

  analyseCopyright(id: number): Observable<HttpResponse<ICopyright>> {
    return this.http.get<ICopyright>(`${this.resourceUrl}/${id}/analyse-copyright`, { observe: 'response' });
  }

  analyseWithFossology(id: number): Observable<HttpResponse<{}>> {
    return this.http.get(`${this.resourceUrl}/${id}/analyse-with-fossology`, { observe: 'response' });
  }

  fossologyAnalysis(id: number): Observable<HttpResponse<IFossology>> {
    return this.http.get<IFossology>(`${this.resourceUrl}/${id}/fossology-analysis`, { observe: 'response' });
  }

  hasErrors(library: ILibrary): boolean {
    for (const errorLog of library.errorLogs ?? []) {
      if (errorLog.status === LogStatus.OPEN) {
        return true;
      }
    }

    return false;
  }

  addLibraryToCollectionIfMissing(libraryCollection: ILibrary[], ...librariesToCheck: (ILibrary | null | undefined)[]): ILibrary[] {
    const libraries: ILibrary[] = librariesToCheck.filter(isPresent);
    if (libraries.length > 0) {
      const libraryCollectionIdentifiers = libraryCollection.map(libraryItem => getLibraryIdentifier(libraryItem)!);
      const librariesToAdd = libraries.filter(libraryItem => {
        const libraryIdentifier = getLibraryIdentifier(libraryItem);
        if (libraryIdentifier == null || libraryCollectionIdentifiers.includes(libraryIdentifier)) {
          return false;
        }
        libraryCollectionIdentifiers.push(libraryIdentifier);
        return true;
      });
      return [...librariesToAdd, ...libraryCollection];
    }
    return libraryCollection;
  }

  protected convertDateFromClient(library: ILibrary): ILibrary {
    return Object.assign({}, library, {
      lastReviewedDate: library.lastReviewedDate?.isValid() ? library.lastReviewedDate.format(DATE_FORMAT) : undefined,
      lastReviewedDeepScanDate: library.lastReviewedDeepScanDate?.isValid()
        ? library.lastReviewedDeepScanDate.format(DATE_FORMAT)
        : undefined,
      createdDate: library.createdDate?.isValid() ? library.createdDate.format(DATE_FORMAT) : undefined,
    });
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.lastReviewedDate = res.body.lastReviewedDate ? dayjs(res.body.lastReviewedDate) : undefined;
      res.body.lastReviewedDeepScanDate = res.body.lastReviewedDeepScanDate ? dayjs(res.body.lastReviewedDeepScanDate) : undefined;
      res.body.createdDate = res.body.createdDate ? dayjs(res.body.createdDate) : undefined;
      res.body.errorLogs?.forEach((libraryErrorLog: ILibraryErrorLog) => {
        libraryErrorLog.timestamp = libraryErrorLog.timestamp ? dayjs(libraryErrorLog.timestamp) : undefined;
      });
      if (res.body.fossology) {
        res.body.fossology.lastScan = res.body.fossology.lastScan ? dayjs(res.body.fossology.lastScan) : undefined;
      }
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((library: ILibrary) => {
        library.lastReviewedDate = library.lastReviewedDate ? dayjs(library.lastReviewedDate) : undefined;
        library.lastReviewedDeepScanDate = library.lastReviewedDeepScanDate ? dayjs(library.lastReviewedDeepScanDate) : undefined;
        library.createdDate = library.createdDate ? dayjs(library.createdDate) : undefined;
        library.errorLogs?.forEach((libraryErrorLog: ILibraryErrorLog) => {
          libraryErrorLog.timestamp = libraryErrorLog.timestamp ? dayjs(libraryErrorLog.timestamp) : undefined;
        });
        if (library.fossology) {
          library.fossology.lastScan = library.fossology.lastScan ? dayjs(library.fossology.lastScan) : undefined;
        }
      });
    }
    return res;
  }
}
