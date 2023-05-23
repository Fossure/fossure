import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IDependency } from '../dependency.model';
import { DependencyService } from '../service/dependency.service';

@Component({
  templateUrl: './dependency-delete-dialog.component.html',
})
export class DependencyDeleteDialogComponent {
  dependency?: IDependency;

  constructor(protected dependencyService: DependencyService, protected activeModal: NgbActiveModal) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.dependencyService.delete(id).subscribe(() => {
      this.activeModal.close('deleted');
    });
  }
}
