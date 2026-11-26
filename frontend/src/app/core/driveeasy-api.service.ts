import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { of, tap } from 'rxjs';
import { Client, ClientInput, Eligibility, EnrichedRental, PriceQuote, Rental, RentalPage, RentalStats, Vehicle, VehicleInput } from './models';

/** The browser API base URL points to the gateway, not to individual services. */
@Injectable({ providedIn: 'root' })
export class DriveEasyApi {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api';
  private readonly vehicleCache = new Map<number, Vehicle>();
  private readonly clientCache = new Map<number, Client>();
  vehicles(filters: { categorie?: string; carburant?: string; statut?: string; prixMax?: number } = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) if (value !== undefined && value !== '') params = params.set(key, value);
    return this.http.get<Vehicle[]>(`${this.base}/vehicules`, { params }).pipe(
      tap(vehicles => vehicles.forEach(vehicle => this.vehicleCache.set(vehicle.id, vehicle))),
    );
  }
  availableVehicles(dateDebut: string, dateFin: string) { return this.http.get<Vehicle[]>(`${this.base}/vehicules/disponibles`, { params: { dateDebut, dateFin } }); }
  vehicle(id: number) {
    const cached = this.vehicleCache.get(id);
    return cached ? of(cached) : this.http.get<Vehicle>(`${this.base}/vehicules/${id}`).pipe(
      tap(vehicle => this.vehicleCache.set(vehicle.id, vehicle)),
    );
  }
  saveVehicle(data: VehicleInput, id?: number) {
    const request = id
      ? this.http.put<Vehicle>(`${this.base}/vehicules/${id}`, data)
      : this.http.post<Vehicle>(`${this.base}/vehicules`, data);
    return request.pipe(tap(vehicle => this.vehicleCache.set(vehicle.id, vehicle)));
  }
  clients(query = '') {
    const value = query.trim();
    const params: Record<string, string> = {};
    if (value) params[value.includes('@') ? 'email' : /^\d+$/.test(value) ? 'cin' : 'nom'] = value;
    return this.http.get<Client[]>(`${this.base}/clients`, { params }).pipe(
      tap(clients => clients.forEach(client => this.clientCache.set(client.id, client))),
    );
  }
  client(id: number) {
    const cached = this.clientCache.get(id);
    return cached ? of(cached) : this.http.get<Client>(`${this.base}/clients/${id}`).pipe(
      tap(client => this.clientCache.set(client.id, client)),
    );
  }
  saveClient(data: ClientInput, id?: number) {
    const request = id
      ? this.http.put<Client>(`${this.base}/clients/${id}`, data)
      : this.http.post<Client>(`${this.base}/clients`, data);
    return request.pipe(tap(client => this.clientCache.set(client.id, client)));
  }
  eligibility(id: number) { return this.http.get<Eligibility>(`${this.base}/clients/${id}/eligibilite`); }
  rentals(filters: { page?: number; size?: number; statut?: string; clientId?: number; vehiculeId?: number; dateDebut?: string; dateFin?: string } = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) if (value !== undefined && value !== '') params = params.set(key, value);
    return this.http.get<RentalPage>(`${this.base}/locations`, { params });
  }
  rental(id: number) { return this.http.get<EnrichedRental>(`${this.base}/locations/${id}`); }
  simulate(data: { clientId: number; vehiculeId: number; dateDebut: string; dateFinPrevue: string }) { return this.http.post<PriceQuote>(`${this.base}/locations/simulation`, data); }
  createRental(data: { clientId: number; vehiculeId: number; dateDebut: string; dateFinPrevue: string }) { return this.http.post<Rental>(`${this.base}/locations`, data); }
  startRental(id: number, kmDepart: number) { return this.http.patch<Rental>(`${this.base}/locations/${id}/depart`, { kmDepart }); }
  returnRental(id: number, kmRetour: number) { return this.http.patch<Rental>(`${this.base}/locations/${id}/retour`, { kmRetour }); }
  cancelRental(id: number) { return this.http.patch<Rental>(`${this.base}/locations/${id}/annulation`, {}); }
  statistics() { return this.http.get<RentalStats>(`${this.base}/locations/statistiques`); }
}
