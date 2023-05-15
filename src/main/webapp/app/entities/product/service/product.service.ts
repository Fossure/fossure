import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { isPresent } from 'app/core/util/operators';
import { IProduct, getProductIdentifier } from '../product.model';

import { IFile } from 'app/core/file/file.model';
import { IUpload } from 'app/core/upload/upload.model';
import { ILibrary } from 'app/entities/library/library.model';
import { IBasicAuth } from 'app/shared/auth/basic-auth.model';
import { ICountOccurrences } from 'app/shared/statistics/count-occurrences.model';
import { IProductOverview } from 'app/shared/statistics/product-overview/product-overview.model';
import { IDifferenceView } from '../../../shared/modals/difference-view-modal/difference-view.model';
import { IProductStatistic } from '../../../shared/statistics/product-overview/product-statistic.model';

export type EntityResponseType = HttpResponse<IProduct>;
export type EntityArrayResponseType = HttpResponse<IProduct[]>;

@Injectable({ providedIn: 'root' })
export class ProductService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/v1/products');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(product: IProduct): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(product);
    return this.http
      .post<IProduct>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(product: IProduct): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(product);
    return this.http
      .put<IProduct>(`${this.resourceUrl}/${getProductIdentifier(product) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  partialUpdate(product: IProduct): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(product);
    return this.http
      .patch<IProduct>(`${this.resourceUrl}/${getProductIdentifier(product) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IProduct>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IProduct[]>(this.resourceUrl, { params: options, observe: 'response' })
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

  transferToTarget(id: number): Observable<any> {
    return this.http.put<any>(`${this.resourceUrl}/${id}/transfer`, { observe: 'response' });
  }

  overview(id: number): Observable<HttpResponse<IProductOverview>> {
    return this.http.get<IProductOverview>(`${this.resourceUrl}/${id}/overview`, { observe: 'response' });
  }

  statistic(id: number): Observable<HttpResponse<IProductStatistic>> {
    return this.http.get<IProductStatistic>(`${this.resourceUrl}/${id}/statistic`, { observe: 'response' });
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
      .post<IProduct>(`${this.resourceUrl}/${id}/create-next-version`, null, { params: options, observe: 'response' })
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
    return this.http.post<IProduct>(`${this.resourceUrl}/${id}/add-libraries`, libraries, { observe: 'response' });
  }

  compareProducts(req?: any): Observable<HttpResponse<IDifferenceView>> {
    const options = createRequestOption(req);
    return this.http.get<IDifferenceView>(`${this.resourceUrl}/compare`, { params: options, observe: 'response' });
  }

  addProductToCollectionIfMissing(productCollection: IProduct[], ...productsToCheck: (IProduct | null | undefined)[]): IProduct[] {
    const products: IProduct[] = productsToCheck.filter(isPresent);
    if (products.length > 0) {
      const productCollectionIdentifiers = productCollection.map(productItem => getProductIdentifier(productItem)!);
      const productsToAdd = products.filter(productItem => {
        const productIdentifier = getProductIdentifier(productItem);
        if (productIdentifier == null || productCollectionIdentifiers.includes(productIdentifier)) {
          return false;
        }
        productCollectionIdentifiers.push(productIdentifier);
        return true;
      });
      return [...productsToAdd, ...productCollection];
    }
    return productCollection;
  }

  protected convertDateFromClient(product: IProduct): IProduct {
    return Object.assign({}, product, {
      createdDate: product.createdDate?.isValid() ? product.createdDate.format(DATE_FORMAT) : undefined,
      lastUpdatedDate: product.lastUpdatedDate?.isValid() ? product.lastUpdatedDate.format(DATE_FORMAT) : undefined,
      deliveredDate: product.deliveredDate?.isValid() ? product.deliveredDate.toJSON() : undefined,
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
      res.body.forEach((product: IProduct) => {
        product.createdDate = product.createdDate ? dayjs(product.createdDate) : undefined;
        product.lastUpdatedDate = product.lastUpdatedDate ? dayjs(product.lastUpdatedDate) : undefined;
        product.deliveredDate = product.deliveredDate ? dayjs(product.deliveredDate) : undefined;
      });
    }
    return res;
  }
}
