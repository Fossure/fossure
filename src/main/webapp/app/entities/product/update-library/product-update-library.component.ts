import { HttpResponse } from '@angular/common/http';
import { Component, Input, TemplateRef } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgbOffcanvas } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { EventManager } from '../../../core/util/event-manager.service';
import { ILibraryPerProduct, LibraryPerProduct } from '../../library-per-product/library-per-product.model';
import { LibraryPerProductService } from '../../library-per-product/service/library-per-product.service';
import { ProductService } from '../service/product.service';

@Component({
  selector: 'jhi-product-update-library',
  templateUrl: './product-update-library.component.html',
})
export class ProductUpdateLibraryComponent {
  closeResult = '';
  isSaving = false;
  saved = false;

  @Input()
  set libraryPerProduct(libraryPerProduct: ILibraryPerProduct | undefined) {
    this.saved = false;
    this._libraryPerProduct = libraryPerProduct;
    this.updateForm(this._libraryPerProduct!);
  }

  get libraryPerProduct(): ILibraryPerProduct | undefined {
    return this._libraryPerProduct;
  }

  libraryPerProductForm = this.fb.group({
    hide: [],
    addedManually: [],
    comment: [],
  });

  private _libraryPerProduct?: ILibraryPerProduct;

  constructor(
    protected offcanvasService: NgbOffcanvas,
    protected eventManager: EventManager,
    protected productService: ProductService,
    protected activatedRoute: ActivatedRoute,
    protected fb: UntypedFormBuilder,
    protected libraryPerProductService: LibraryPerProductService
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
    const libraryPerProduct = this.createFromForm();
    this.subscribeToSaveResponse(this.libraryPerProductService.partialUpdate(libraryPerProduct));
  }

  protected updateForm(libraryPerProduct: ILibraryPerProduct): void {
    this.libraryPerProductForm.patchValue({
      hide: libraryPerProduct.hideForPublishing,
      addedManually: libraryPerProduct.addedManually,
    });
  }

  protected createFromForm(): ILibraryPerProduct {
    return {
      ...new LibraryPerProduct(),
      id: this.libraryPerProduct?.id,
      hideForPublishing: this.libraryPerProductForm.get(['hide'])!.value,
      addedManually: this.libraryPerProductForm.get(['addedManually'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ILibraryPerProduct>>): void {
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
