import { Component, OnInit, TemplateRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ILicense } from '../license.model';
import { DataUtils } from 'app/core/util/data-util.service';
import { HttpResponse } from '@angular/common/http';
import { ILicenseConflict } from '../../license-conflict/license-conflict.model';
import { LicenseService } from '../service/license.service';
import { ILicenseRisk } from '../../license-risk/license-risk.model';
import { map } from 'rxjs/operators';
import { LicenseRiskService } from '../../license-risk/service/license-risk.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-license-detail',
  templateUrl: './license-detail.component.html',
})
export class LicenseDetailComponent implements OnInit {
  license: ILicense | null = null;  
  
  isLoadingLicenseConflicts = false;
  finishedLoadingLicenseConflicts = false;

  licenseConflictsMap = new Map<string, ILicenseConflict[]>();
  licenseRisksSharedCollection: ILicenseRisk[] = [];

  constructor(
    protected dataUtils: DataUtils,
    protected activatedRoute: ActivatedRoute,
    protected licenseService: LicenseService,
    protected licenseRiskService: LicenseRiskService,
    protected modalService: NgbModal
  ) {}
  
  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ license }) => {
      this.license = license;
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  previousState(): void {
    window.history.back();
  }

  navTabChange(event: any): void {
    this.isLoadingLicenseConflicts = true;

    if (event.nextId === 'License Conflicts' && !this.finishedLoadingLicenseConflicts && this.license?.id) {
      this.licenseService.fetchLicenseConflictsWithRisk(this.license.id).subscribe({
        next: (res: HttpResponse<ILicenseConflict[]>) => {
          this.licenseRiskService
            .query()
            .pipe(map((resRisk: HttpResponse<ILicenseRisk[]>) => resRisk.body ?? []))
            .subscribe((licenseRisks: ILicenseRisk[]) => {
              this.licenseRisksSharedCollection = licenseRisks;

              res.body?.forEach(value => {
                if (value.secondLicenseConflict?.licenseRisk?.name) {
                  const licenseRiskName = value.secondLicenseConflict.licenseRisk.name;
                  if (this.licenseConflictsMap.has(licenseRiskName)) {
                    this.licenseConflictsMap.get(licenseRiskName)!.push(value);
                  } else {
                    this.licenseConflictsMap.set(licenseRiskName, [value]);
                  }
                }
              });

              this.licenseConflictsMap = new Map<string, ILicenseConflict[]>(
                [...this.licenseConflictsMap].sort((a, b) => {
                  if (
                    this.licenseRisksSharedCollection.find(value => value.name === a[0])!.level! >
                    this.licenseRisksSharedCollection.find(value => value.name === b[0])!.level!
                  ) {
                    return 1;
                  } else if (
                    this.licenseRisksSharedCollection.find(value => value.name === a[0])!.level! <
                    this.licenseRisksSharedCollection.find(value => value.name === b[0])!.level!
                  ) {
                    return -1;
                  }
                  return 0;
                })
              );

              this.isLoadingLicenseConflicts = false;
              this.finishedLoadingLicenseConflicts = true;
            });
        },
        error: () => (this.isLoadingLicenseConflicts = false),
      });
    }
  }

  asIsOrder(): number {
    return 0;
  }

  licenseRiskColor(name: string): string | null | undefined {
    return this.licenseRisksSharedCollection.find(value => value.name === name)?.color;
  }

  openScrollableContent(longContent: TemplateRef<any>, windowSize: string): void {
    this.modalService.open(longContent, { scrollable: true, size: windowSize });
  }
}
