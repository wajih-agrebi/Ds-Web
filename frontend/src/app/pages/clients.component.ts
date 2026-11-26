import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { Client } from '../core/models';

@Component({ standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './clients.component.html' })
export class ClientsComponent implements OnInit {
  private readonly api = inject(DriveEasyApi);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private requestId = 0;
  clients: Client[] = []; query = ''; loading = true; error = '';
  ngOnInit() { this.load(); }
  load() {
    const requestId = ++this.requestId;
    this.loading = true;
    this.error = '';
    this.api.clients(this.query.trim()).pipe(timeout({ first: 8000 })).subscribe({
      next: clients => {
        if (requestId !== this.requestId) return;
        this.clients = clients;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        if (requestId !== this.requestId) return;
        this.error = error?.name === 'TimeoutError'
          ? 'The client service did not respond. Make sure the Service Registry, Config Server, Client Service, and API Gateway are running, then retry.'
          : error?.status === 0
            ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
            : error?.error?.error || 'Unable to load clients. Check the backend services, then retry.';
        this.loading = false;
        this.changeDetector.markForCheck();
      },
    });
  }
  expired(client: Client) { return client.dateExpirationPermis < new Date().toISOString().slice(0, 10); }
}
