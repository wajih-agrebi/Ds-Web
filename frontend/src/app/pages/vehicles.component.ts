import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { Vehicle } from '../core/models';
import { suggestedVehiclePhoto } from '../core/vehicle-images';

@Component({ standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './vehicles.component.html' })
export class VehiclesComponent implements OnInit {
  private readonly api = inject(DriveEasyApi);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private requestId = 0;
  private readonly failedImageIds = new Set<number>();
  vehicles: Vehicle[] = []; loading = true; error = ''; category = ''; fuel = ''; status = ''; maxPrice: number | null = null; sort = 'make'; page = 0; size = 6;
  ngOnInit() { this.load(); }
  load() {
    const requestId = ++this.requestId;
    this.loading = true; this.error = '';
    this.api.vehicles({ categorie: this.category || undefined, carburant: this.fuel || undefined, statut: this.status || undefined, prixMax: this.maxPrice ?? undefined })
      .pipe(timeout({ first: 8000 }))
      .subscribe({
        next: items => {
          if (requestId !== this.requestId) return;
          this.vehicles = items;
          this.page = 0;
          this.loading = false;
          this.changeDetector.markForCheck();
        },
        error: error => {
          if (requestId !== this.requestId) return;
          this.error = error?.name === 'TimeoutError'
            ? 'The vehicle service did not respond. Make sure the Service Registry, Config Server, Vehicle Service, and API Gateway are running, then retry.'
            : error?.status === 0
              ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
              : error?.error?.error || 'Unable to load vehicles. Check the backend services, then retry.';
          this.loading = false;
          this.changeDetector.markForCheck();
        },
      });
  }
  get ordered() { return [...this.vehicles].sort((a, b) => this.sort === 'price' ? a.prixJournalier - b.prixJournalier : `${a.marque} ${a.modele}`.localeCompare(`${b.marque} ${b.modele}`)); }
  get shown() { return this.ordered.slice(this.page * this.size, (this.page + 1) * this.size); }
  get pages() { return Math.max(1, Math.ceil(this.vehicles.length / this.size)); }
  imageFor(vehicle: Vehicle) { return vehicle.imageUrl?.trim() || suggestedVehiclePhoto(vehicle); }
  showImage(vehicle: Vehicle) { return !!this.imageFor(vehicle) && !this.failedImageIds.has(vehicle.id); }
  imageFailed(vehicleId: number) { this.failedImageIds.add(vehicleId); }
}
