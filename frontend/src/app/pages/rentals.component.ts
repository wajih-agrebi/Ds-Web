import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { Client, RentalPage, RentalStatus, Vehicle } from '../core/models';
import { timeout } from 'rxjs';

@Component({ standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './rentals.component.html' })
export class RentalsComponent implements OnInit {
  private readonly api = inject(DriveEasyApi);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private requestId = 0;
  private mileageRequestId = 0;
  result: RentalPage | null = null; loading = true; error = ''; status = ''; dateDebut = ''; dateFin = ''; page = 0; readonly size = 10; notice = '';
  mileageByVehicle = new Map<number, number>(); vehiclesById = new Map<number, Vehicle>(); clientsById = new Map<number, Client>(); mileageLoading = true; mileageError = ''; startingId: number | null = null;
  ngOnInit() { this.load(); this.loadVehicleMileage(); this.loadClientNames(); }
  load() {
    const requestId = ++this.requestId;
    this.loading = true;
    this.error = '';
    this.api.rentals({ page: this.page, size: this.size, statut: this.status || undefined, dateDebut: this.dateDebut || undefined, dateFin: this.dateFin || undefined })
      .pipe(timeout({ first: 8000 }))
      .subscribe({
        next: result => {
          if (requestId !== this.requestId) return;
          this.result = result;
          this.loading = false;
          this.changeDetector.markForCheck();
        },
        error: error => {
          if (requestId !== this.requestId) return;
          this.error = error?.name === 'TimeoutError'
            ? 'The rental service took too long to respond. Check that it and the API Gateway are running, then retry.'
            : error?.status === 0
              ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
              : error?.error?.error || 'Rentals could not be loaded. Check the backend services, then retry.';
          this.loading = false;
          this.changeDetector.markForCheck();
        },
      });
  }
  get statuses(): RentalStatus[] { return ['RESERVEE', 'EN_COURS', 'TERMINEE', 'ANNULEE']; }
  mileageFor(vehicleId: number) { return this.mileageByVehicle.get(vehicleId) ?? null; }
  clientNameFor(clientId: number) {
    const client = this.clientsById.get(clientId);
    return client ? `${client.prenom} ${client.nom}` : `Client #${clientId}`;
  }
  vehicleNameFor(vehicleId: number) {
    const vehicle = this.vehiclesById.get(vehicleId);
    return vehicle ? `${vehicle.marque} ${vehicle.modele}` : `Vehicle #${vehicleId}`;
  }
  loadClientNames() {
    this.api.clients().pipe(timeout({ first: 8000 })).subscribe({
      next: clients => {
        this.clientsById = new Map(clients.map(client => [client.id, client]));
        this.changeDetector.markForCheck();
      },
      error: () => {
        // Keep the rental list usable with ID labels if the client service is unavailable.
      },
    });
  }
  loadVehicleMileage() {
    const requestId = ++this.mileageRequestId;
    this.mileageLoading = true;
    this.mileageError = '';
    this.api.vehicles().pipe(timeout({ first: 8000 })).subscribe({
      next: vehicles => {
        if (requestId !== this.mileageRequestId) return;
        this.mileageByVehicle = new Map(vehicles.map(vehicle => [vehicle.id, vehicle.kilometrage]));
        this.vehiclesById = new Map(vehicles.map(vehicle => [vehicle.id, vehicle]));
        this.mileageLoading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        if (requestId !== this.mileageRequestId) return;
        this.mileageError = error?.name === 'TimeoutError'
          ? 'Vehicle mileage is taking too long to load. Retry before starting a rental.'
          : error?.error?.error || 'Could not load saved vehicle mileage. Retry before starting a rental.';
        this.mileageLoading = false;
        this.changeDetector.markForCheck();
      },
    });
  }
  start(id: number, mileage: HTMLInputElement) {
    const value = Number(mileage.value);
    const rental = this.result?.content.find(item => item.id === id);
    const minimum = rental ? this.mileageFor(rental.vehiculeId) : null;
    if (minimum === null) { this.error = 'The vehicle’s saved mileage has not loaded yet. Retry the vehicle mileage request first.'; return; }
    if (!Number.isFinite(value) || value < minimum) { this.error = `Enter the actual pickup mileage. It must be at least ${minimum.toLocaleString()} km.`; return; }
    this.startingId = id;
    this.error = '';
    this.api.startRental(id, value).pipe(timeout({ first: 10000 })).subscribe({
      next: () => {
        this.startingId = null;
        this.notice = `Rental #${id} started.`;
        this.load();
        this.loadVehicleMileage();
      },
      error: error => {
        this.startingId = null;
        this.error = error?.name === 'TimeoutError'
          ? 'Starting the rental took too long. Refresh the list to check its status.'
          : error?.error?.error || 'Rental could not be started.';
        this.changeDetector.markForCheck();
      },
    });
  }
  cancel(id: number) { if (!window.confirm(`Cancel reservation #${id}?`)) return; this.api.cancelRental(id).subscribe({ next: () => { this.notice = `Reservation #${id} cancelled.`; this.load(); }, error: e => this.error = e?.error?.error || 'Reservation could not be cancelled.' }); }
}
