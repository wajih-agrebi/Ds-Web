import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { ClientInput } from '../core/models';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './client-form.component.html',
})
export class ClientFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(DriveEasyApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);

  id?: number;
  loading = false;
  loadedClient = false;
  saving = false;
  error = '';

  form = this.fb.group({
    cin: ['', Validators.required],
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    dateNaissance: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['', Validators.required],
    adresse: ['', Validators.required],
    numeroPermis: ['', Validators.required],
    dateExpirationPermis: ['', Validators.required],
  });

  ngOnInit() {
    const rawId = this.route.snapshot.paramMap.get('id');
    if (!rawId) return;
    this.id = Number(rawId);
    this.loadClient();
  }

  loadClient() {
    if (!this.id) return;
    this.loading = true;
    this.error = '';
    this.api.client(this.id).pipe(timeout({ first: 8000 })).subscribe({
      next: client => {
        this.form.patchValue(client);
        this.loadedClient = true;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Loading the client took too long. Check the Client Service and API Gateway, then retry.'
          : error?.status === 0
            ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
            : error?.error?.error || 'Client could not be loaded.';
        this.loading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving) return;

    this.saving = true;
    this.error = '';
    const client = this.form.getRawValue() as ClientInput;
    this.api.saveClient(client, this.id).pipe(timeout({ first: 10000 })).subscribe({
      next: saved => this.router.navigateByUrl(this.id ? `/clients/${saved.id}` : '/clients'),
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Saving took too long. Check the client list before trying again.'
          : error?.error?.error || error?.error?.message || 'Client could not be saved. Check the information and try again.';
        this.saving = false;
        this.changeDetector.markForCheck();
      },
    });
  }
}
