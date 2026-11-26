import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard.component';
import { VehiclesComponent } from './pages/vehicles.component';
import { VehicleFormComponent } from './pages/vehicle-form.component';
import { ClientsComponent } from './pages/clients.component';
import { ClientFormComponent } from './pages/client-form.component';
import { ClientDetailComponent } from './pages/client-detail.component';
import { RentalsComponent } from './pages/rentals.component';
import { RentalNewComponent } from './pages/rental-new.component';
import { RentalReturnComponent } from './pages/rental-return.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent, title: 'Overview · DriveEasy' },
  { path: 'vehicules', component: VehiclesComponent, title: 'Vehicles · DriveEasy' },
  { path: 'vehicules/new', component: VehicleFormComponent, title: 'Add vehicle · DriveEasy' },
  { path: 'vehicules/:id/edit', component: VehicleFormComponent, title: 'Edit vehicle · DriveEasy' },
  { path: 'clients', component: ClientsComponent, title: 'Clients · DriveEasy' },
  { path: 'clients/new', component: ClientFormComponent, title: 'Add client · DriveEasy' },
  { path: 'clients/:id/edit', component: ClientFormComponent, title: 'Edit client · DriveEasy' },
  { path: 'clients/:id', component: ClientDetailComponent, title: 'Client · DriveEasy' },
  { path: 'locations', component: RentalsComponent, title: 'Rentals · DriveEasy' },
  { path: 'locations/new', component: RentalNewComponent, title: 'New rental · DriveEasy' },
  { path: 'locations/:id/retour', component: RentalReturnComponent, title: 'Vehicle return · DriveEasy' },
  { path: '**', redirectTo: 'dashboard' },
];
