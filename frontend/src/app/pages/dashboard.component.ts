import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, switchMap, timeout } from 'rxjs';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { Client, RentalStats, Vehicle, RentalPage } from '../core/models';

@Component({ standalone: true, imports: [CommonModule, RouterLink], templateUrl: './dashboard.component.html' })
export class DashboardComponent implements OnInit {
  private readonly api = inject(DriveEasyApi);
  private readonly changeDetector = inject(ChangeDetectorRef);
  vehicles: Vehicle[] | null = null; clients: Client[] | null = null; rentals: RentalPage | null = null; stats: RentalStats | null = null;
  rentalCounts: { reserved: number | null; inProgress: number | null; completed: number | null; cancelled: number | null } | null = null;
  loading = true; error = '';
  lastUpdated: Date | null = null;
  get available() { return this.vehicles?.filter(v => v.statut === 'DISPONIBLE').length ?? 0; }
  get rented() { return this.vehicles?.filter(v => v.statut === 'LOUE').length ?? 0; }
  get maintenance() { return this.vehicles?.filter(v => v.statut === 'MAINTENANCE').length ?? 0; }
  clientNameFor(clientId: number) {
    const client = this.clients?.find(item => item.id === clientId);
    return client ? `${client.prenom} ${client.nom}` : `Client #${clientId}`;
  }
  vehicleNameFor(vehicleId: number) {
    const vehicle = this.vehicleFor(vehicleId);
    return vehicle ? `${vehicle.marque} ${vehicle.modele}` : `Vehicle #${vehicleId}`;
  }
  vehicleFor(vehicleId: number) { return this.vehicles?.find(item => item.id === vehicleId) ?? null; }
  countActive() {
    const reserved = this.rentalCounts?.reserved;
    const inProgress = this.rentalCounts?.inProgress;
    return reserved === null || reserved === undefined || inProgress === null || inProgress === undefined
      ? null : reserved + inProgress;
  }
  get latestRentals() {
    return [...(this.rentals?.content ?? [])]
      .sort((a, b) => b.dateCreation.localeCompare(a.dateCreation) || b.id - a.id)
      .slice(0, 5);
  }
  get rentalStatusRows() {
    const rows = [
      { label: 'Reserved', status: 'RESERVEE', count: this.rentalCounts?.reserved ?? null },
      { label: 'In progress', status: 'EN_COURS', count: this.rentalCounts?.inProgress ?? null },
      { label: 'Completed', status: 'TERMINEE', count: this.rentalCounts?.completed ?? null },
      { label: 'Cancelled', status: 'ANNULEE', count: this.rentalCounts?.cancelled ?? null },
    ];
    const total = rows.reduce((sum, row) => sum + (row.count ?? 0), 0);
    return rows.map(row => ({ ...row, percent: total && row.count !== null ? row.count / total * 100 : 0 }));
  }
  get categoryRows() {
    const total = this.vehicles?.length ?? 0;
    const counts = new Map<string, number>();
    for (const vehicle of this.vehicles ?? []) counts.set(vehicle.categorie, (counts.get(vehicle.categorie) ?? 0) + 1);
    return [...counts.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([category, count]) => ({ category, count, percent: total ? count / total * 100 : 0 }));
  }
  get popularVehicles() {
    const fleet = this.vehicles ?? [];
    const rows = this.stats?.vehiculesLesPlusLoues ?? [];
    const max = Math.max(1, ...rows.map(row => row.nombreLocations));
    return rows.map(row => {
      const vehicle = fleet.find(item => item.id === row.vehiculeId);
      return {
        id: row.vehiculeId,
        name: vehicle ? `${vehicle.marque} ${vehicle.modele}` : `Vehicle #${row.vehiculeId}`,
        registration: vehicle?.immatriculation ?? '',
        count: row.nombreLocations,
        percent: row.nombreLocations / max * 100,
      };
    });
  }

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading = true;
    this.error = '';
    this.vehicles = null;
    this.clients = null;
    this.rentals = null;
    this.stats = null;
    this.rentalCounts = null;

    forkJoin({
      vehicles: this.load(this.api.vehicles()),
      clients: this.load(this.api.clients()),
      rentals: this.loadLatestRentals(),
      stats: this.load(this.api.statistics()),
      rentalCounts: this.loadRentalCounts(),
    }).subscribe(({ vehicles, clients, rentals, stats, rentalCounts }) => {
      this.vehicles = vehicles.value;
      this.clients = clients.value;
      this.rentals = rentals.value;
      this.stats = stats.value;
      this.rentalCounts = {
        reserved: rentalCounts.reserved.value,
        inProgress: rentalCounts.inProgress.value,
        completed: rentalCounts.completed.value,
        cancelled: rentalCounts.cancelled.value,
      };

      const unavailable = [
        vehicles.error && 'fleet',
        clients.error && 'clients',
        rentals.error && 'rentals',
        stats.error && 'statistics',
        Object.values(rentalCounts).some(result => result.error) && 'rental counts',
      ].filter(Boolean);
      if (unavailable.length) {
        this.error = `Could not load ${unavailable.join(', ')} data. Check that the backend services are running, then retry.`;
      }
      this.lastUpdated = new Date();
      this.loading = false;
      this.changeDetector.markForCheck();
    });
  }

  private loadLatestRentals() {
    return this.load(this.api.rentals({ page: 0, size: 100 })).pipe(
      switchMap(firstPage => {
        if (!firstPage.value || firstPage.value.totalPages <= 1) return of(firstPage);
        const lastPage = firstPage.value.totalPages - 1;
        return this.load(this.api.rentals({ page: lastPage, size: 100 })).pipe(
          map(latestPage => ({ value: latestPage.value ?? firstPage.value, error: latestPage.error })),
        );
      }),
    );
  }

  private loadRentalCounts() {
    return forkJoin({
      reserved: this.loadRentalCount('RESERVEE'),
      inProgress: this.loadRentalCount('EN_COURS'),
      completed: this.loadRentalCount('TERMINEE'),
      cancelled: this.loadRentalCount('ANNULEE'),
    });
  }

  private loadRentalCount(status: 'RESERVEE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE') {
    return this.load(this.api.rentals({ page: 0, size: 1, statut: status })).pipe(
      map(result => ({ ...result, value: result.value?.totalElements ?? null })),
    );
  }

  private load<T>(request: Observable<T>) {
    return request.pipe(
      timeout({ first: 8000 }),
      map(value => ({ value, error: false })),
      catchError(() => of({ value: null, error: true })),
    );
  }
}
