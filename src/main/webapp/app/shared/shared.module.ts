import { NgModule } from '@angular/core';
import { SharedLibsModule } from './shared-libs.module';
import { AlertComponent } from './alert/alert.component';
import { AlertErrorComponent } from './alert/alert-error.component';
import { HasAnyAuthorityDirective } from './auth/has-any-authority.directive';
import { DurationPipe } from './date/duration.pipe';
import { FormatMediumDatetimePipe } from './date/format-medium-datetime.pipe';
import { FormatMediumDatePipe } from './date/format-medium-date.pipe';
import { FormatSimpleDatetimePipe } from './date/format-simple-datetime.pipe';
import { FormatSimpleDatePipe } from './date/format-simple-date.pipe';
import { SortByDirective } from './sort/sort-by.directive';
import { SortDirective } from './sort/sort.directive';
import { ItemCountComponent } from './pagination/item-count.component';
import { CopyrightModalComponent } from './modals/copyright-modal/copyright-modal.component';
import { UrlTransformationPipe } from './url/string-to-url.pipe';
import { LibraryLabelPipe } from './label/library-label.pipe';
import { FirstCapitalLetterPipe } from './text/first-capital-letter.pipe';
import { ShortenedNumberPipe } from './number/shortened-number.pipe';
import { NotificationComponent } from './notification/notification/notification.component';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [SharedLibsModule, RouterModule],
  declarations: [
    AlertComponent,
    AlertErrorComponent,
    HasAnyAuthorityDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    FormatSimpleDatetimePipe,
    FormatSimpleDatePipe,
    SortByDirective,
    SortDirective,
    ItemCountComponent,
    CopyrightModalComponent,
    UrlTransformationPipe,
    LibraryLabelPipe,
    FirstCapitalLetterPipe,
    ShortenedNumberPipe,
    NotificationComponent,
  ],
  exports: [
    SharedLibsModule,
    AlertComponent,
    AlertErrorComponent,
    HasAnyAuthorityDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    FormatSimpleDatetimePipe,
    FormatSimpleDatePipe,
    SortByDirective,
    SortDirective,
    ItemCountComponent,
    UrlTransformationPipe,
    LibraryLabelPipe,
    FirstCapitalLetterPipe,
    ShortenedNumberPipe,
    NotificationComponent,
  ],
})
export class SharedModule {}
