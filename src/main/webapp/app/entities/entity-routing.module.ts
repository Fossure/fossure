import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'library',
        data: { pageTitle: 'Libraries' },
        loadChildren: () => import('./library/library.module').then(m => m.LibraryModule),
      },
      {
        path: 'license',
        data: { pageTitle: 'Licenses' },
        loadChildren: () => import('./license/license.module').then(m => m.LicenseModule),
      },
      {
        path: 'license-risk',
        data: { pageTitle: 'LicenseRisks' },
        loadChildren: () => import('./license-risk/license-risk.module').then(m => m.LicenseRiskModule),
      },
      {
        path: 'requirement',
        data: { pageTitle: 'Requirements' },
        loadChildren: () => import('./requirement/requirement.module').then(m => m.RequirementModule),
      },
      {
        path: 'product',
        data: { pageTitle: 'Products' },
        loadChildren: () => import('./product/product.module').then(m => m.ProductModule),
      },
      {
        path: 'license-naming-mapping',
        data: {
          authorities: ['ROLE_USER', 'ROLE_ADMIN'],
          pageTitle: 'LicenseNamingMappings',
        },
        loadChildren: () => import('./license-naming-mapping/license-naming-mapping.module').then(m => m.LicenseNamingMappingModule),
      },
      {
        path: 'generic-license-url',
        data: {
          authorities: ['ROLE_USER', 'ROLE_ADMIN'],
          pageTitle: 'GenericLicenseUrls',
        },
        loadChildren: () => import('./generic-license-url/generic-license-url.module').then(m => m.GenericLicenseUrlModule),
      },
      {
        path: 'upload',
        data: {
          authorities: ['ROLE_USER', 'ROLE_ADMIN'],
          pageTitle: 'Uploads',
        },
        loadChildren: () => import('./upload/upload.module').then(m => m.UploadModule),
      },
      {
        path: 'library-per-product',
        loadChildren: () => import('./library-per-product/library-per-product.module').then(m => m.LibraryPerProductModule),
      },
    ]),
  ],
})
export class EntityRoutingModule {}
