import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { IInsights } from 'app/home/home.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class HomeService {
  public resourceUrl = this.applicationConfigService.getEndpointFor('api/v1/dashboard');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  getInsights(): Observable<HttpResponse<IInsights>> {
    return this.http.get<IInsights>(`${this.resourceUrl}/insights`, { observe: 'response' });
  }
}
