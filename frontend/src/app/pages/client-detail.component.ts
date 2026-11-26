import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { Client, Eligibility, RentalPage, Vehicle } from '../core/models';

@Component({ standalone: true, imports: [CommonModule, RouterLink], templateUrl: './client-detail.component.html' })
export class ClientDetailComponent implements OnInit {
  private readonly api = inject(DriveEasyApi);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);

  id = 0;
  client: Client | null = null;
  eligibility: Eligibility | null = null;
  history: RentalPage | null = null;
  vehiclesById = new Map<number, Vehicle>();
  clientLoading = true;
  eligibilityLoading = true;
  historyLoading = true;
  clientError = '';
  eligibilityError = '';
  historyError = '';

  ngOnInit() {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadClient();
    this.loadEligibility();
    this.loadHistory();
    this.loadVehicles();
  }

  vehicleNameFor(vehicleId: number) {
    const vehicle = this.vehiclesById.get(vehicleId);
    return vehicle ? `${vehicle.marque} ${vehicle.modele}` : `Vehicle #${vehicleId}`;
  }

  private loadVehicles() {
    this.api.vehicles().pipe(timeout({ first: 8000 })).subscribe({
      next: vehicles => {
        this.vehiclesById = new Map(vehicles.map(vehicle => [vehicle.id, vehicle]));
        this.changeDetector.markForCheck();
      },
      error: () => this.changeDetector.markForCheck(),
    });
  }

  loadClient() {
    this.clientLoading = true;
    this.clientError = '';
    this.api.client(this.id).pipe(timeout({ first: 8000 })).subscribe({
      next: client => {
        this.client = client;
        this.clientLoading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.clientError = error?.name === 'TimeoutError'
          ? 'Loading the client took too long. Retry, or check the Client Service and API Gateway.'
          : error?.status === 0
            ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
            : error?.error?.error || 'Client could not be loaded.';
        this.clientLoading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  loadEligibility() {
    this.eligibilityLoading = true;
    this.eligibilityError = '';
    this.api.eligibility(this.id).pipe(timeout({ first: 8000 })).subscribe({
      next: eligibility => {
        this.eligibility = eligibility;
        this.eligibilityLoading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.eligibilityError = error?.name === 'TimeoutError'
          ? 'Eligibility check took too long.'
          : error?.error?.error || 'Eligibility could not be checked.';
        this.eligibilityLoading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  loadHistory() {
    this.historyLoading = true;
    this.historyError = '';
    this.api.rentals({ page: 0, size: 100, clientId: this.id }).pipe(timeout({ first: 8000 })).subscribe({
      next: history => {
        this.history = history;
        this.historyLoading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.historyError = error?.name === 'TimeoutError'
          ? 'Rental history took too long to load.'
          : error?.error?.error || 'Rental history could not be loaded.';
        this.historyLoading = false;
        this.changeDetector.markForCheck();
      },
    });
  }
}
