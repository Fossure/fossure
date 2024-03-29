import { Component, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { UntypedFormBuilder, Validators } from '@angular/forms';

import { EMAIL_ALREADY_USED_KEY, LOGIN_ALREADY_USED_KEY, EMAIL_DOMAIN_INVALID_KEY } from 'app/config/error.constants';
import { RegisterService } from './register.service';

@Component({
  selector: 'jhi-register',
  templateUrl: './register.component.html',
})
export class RegisterComponent implements AfterViewInit {
  @ViewChild('login', { static: false })
  login?: ElementRef;

  doNotMatch = false;
  error = false;
  errorEmailExists = false;
  errorUserExists = false;
  errorEmailDomainInvalid = false;
  errorEmailDomainInvalidText = '';
  success = false;

  registerForm = this.fb.group({
    login: [
      '',
      [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    ],
    email: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
  });

  constructor(private registerService: RegisterService, private fb: UntypedFormBuilder) {}

  ngAfterViewInit(): void {
    if (this.login) {
      this.login.nativeElement.focus();
    }
  }

  register(): void {
    this.doNotMatch = false;
    this.error = false;
    this.errorEmailExists = false;
    this.errorUserExists = false;
    this.errorEmailDomainInvalid = false;
    this.errorEmailDomainInvalidText = '';

    const password = this.registerForm.get(['password'])!.value;
    if (password !== this.registerForm.get(['confirmPassword'])!.value) {
      this.doNotMatch = true;
    } else {
      const login = this.registerForm.get(['login'])!.value;
      const email = this.registerForm.get(['email'])!.value;
      this.registerService
        .save({ login, email, password, langKey: 'en' })
        .subscribe({ next: () => (this.success = true), error: response => this.processError(response) });
    }
  }

  private processError(response: HttpErrorResponse): void {
    if (response.status === 400 && response.error.errorKey === LOGIN_ALREADY_USED_KEY) {
      this.errorUserExists = true;
    } else if (response.status === 400 && response.error.errorKey === EMAIL_ALREADY_USED_KEY) {
      this.errorEmailExists = true;
    } else if (response.status === 400 && response.error.errorKey === EMAIL_DOMAIN_INVALID_KEY) {
      this.errorEmailDomainInvalid = true;
      this.errorEmailDomainInvalidText = response.error.title;
    } else {
      this.error = true;
    }
  }
}
