import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, timeout } from 'rxjs';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { Client, Eligibility, PriceQuote, Vehicle } from '../core/models';

function localDate(date: Date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }

@Component({ standalone: true, imports: [CommonModule, ReactiveFormsModule, RouterLink], templateUrl: './rental-new.component.html' })
export class RentalNewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(DriveEasyApi);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);

  clients: Client[] = [];
  eligible: Eligibility | null = null;
  available: Vehicle[] = [];
  vehicle: Vehicle | null = null;
  quote: PriceQuote | null = null;
  step = 1;
  loading = false;
  clientsLoading = true;
  saving = false;
  loadingMessage = '';
  error = '';

  form = this.fb.group({
    clientId: [null as number | null, Validators.required],
    dateDebut: [localDate(new Date()), Validators.required],
    dateFinPrevue: [localDate(new Date(Date.now() + 86400000)), Validators.required],
  });

  ngOnInit() {
    this.api.clients().pipe(timeout({ first: 8000 })).subscribe({
      next: clients => {
        this.clients = clients;
        this.clientsLoading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Loading clients took too long. Check the backend services and retry.'
          : error?.status === 0
            ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
            : error?.error?.error || 'Clients could not be loaded.';
        this.clientsLoading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  get client() { return this.clients.find(c => c.id === this.form.controls.clientId.value) ?? null; }
  get today() { return localDate(new Date()); }

  checkClient() {
    this.form.controls.clientId.markAsTouched();
    if (this.form.controls.clientId.invalid) return;
    const { dateDebut, dateFinPrevue } = this.form.getRawValue();
    if (!dateDebut || !dateFinPrevue || dateFinPrevue < dateDebut) {
      this.error = 'The return date must be the same as or later than pickup.';
      return;
    }

    this.loading = true;
    this.loadingMessage = 'Checking eligibility and vehicle availability…';
    this.error = '';
    this.eligible = null;
    this.vehicle = null;
    this.quote = null;

    forkJoin({
      eligibility: this.api.eligibility(this.form.controls.clientId.value!).pipe(timeout({ first: 8000 })),
      vehicles: this.api.availableVehicles(dateDebut, dateFinPrevue).pipe(timeout({ first: 8000 })),
    }).subscribe({
      next: ({ eligibility, vehicles }) => {
        this.eligible = eligibility;
        this.available = vehicles;
        this.loading = false;
        this.loadingMessage = '';
        if (eligibility.eligible) this.step = 2;
        else this.error = eligibility.message;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Checking the reservation took too long. Please try again.'
          : error?.status === 0
            ? 'Cannot reach a backend service through localhost:8080. Check that the services are running, then retry.'
            : error?.error?.error || 'Eligibility or vehicle availability could not be checked.';
        this.loading = false;
        this.loadingMessage = '';
        this.changeDetector.markForCheck();
      },
    });
  }

  findVehicles() {
    const { dateDebut, dateFinPrevue } = this.form.getRawValue();
    if (!dateDebut || !dateFinPrevue || dateFinPrevue < dateDebut) {
      this.error = 'The return date must be the same as or later than pickup.';
      return;
    }
    this.loading = true;
    this.loadingMessage = 'Refreshing availability…';
    this.error = '';
    this.vehicle = null;
    this.quote = null;
    this.api.availableVehicles(dateDebut, dateFinPrevue).pipe(timeout({ first: 8000 })).subscribe({
      next: vehicles => {
        this.available = vehicles;
        this.loading = false;
        this.loadingMessage = '';
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Vehicle availability took too long. Please try again.'
          : error?.error?.error || 'Available vehicles could not be loaded.';
        this.loading = false;
        this.loadingMessage = '';
        this.changeDetector.markForCheck();
      },
    });
  }

  choose(vehicle: Vehicle) { this.vehicle = vehicle; }

  simulate() {
    if (!this.vehicle) return;
    this.loading = true;
    this.loadingMessage = 'Preparing your price estimate…';
    this.error = '';
    const { dateDebut, dateFinPrevue } = this.form.getRawValue();
    this.api.simulate({ clientId: this.form.controls.clientId.value!, vehiculeId: this.vehicle.id, dateDebut: dateDebut!, dateFinPrevue: dateFinPrevue! })
      .pipe(timeout({ first: 8000 }))
      .subscribe({
        next: quote => {
          this.quote = quote;
          this.loading = false;
          this.loadingMessage = '';
          this.step = 3;
          this.changeDetector.markForCheck();
        },
        error: error => {
          this.error = error?.name === 'TimeoutError'
            ? 'The price estimate took too long. Please try again.'
            : error?.error?.error || 'Price simulation failed.';
          this.loading = false;
          this.loadingMessage = '';
          this.changeDetector.markForCheck();
        },
      });
  }

  confirm() {
    if (!this.vehicle || this.saving) return;
    this.saving = true;
    this.error = '';
    const { dateDebut, dateFinPrevue } = this.form.getRawValue();
    this.api.createRental({ clientId: this.form.controls.clientId.value!, vehiculeId: this.vehicle.id, dateDebut: dateDebut!, dateFinPrevue: dateFinPrevue! })
      .pipe(timeout({ first: 10000 }))
      .subscribe({
        next: () => this.router.navigateByUrl('/locations'),
        error: error => {
          this.error = error?.name === 'TimeoutError'
            ? 'Saving the reservation took too long. Check the rentals list before trying again.'
            : error?.error?.error || 'Reservation could not be saved.';
          this.saving = false;
          this.changeDetector.markForCheck();
        },
      });
  }
}
