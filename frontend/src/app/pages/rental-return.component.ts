import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { EnrichedRental, Rental } from '../core/models';

@Component({ standalone: true, imports: [CommonModule, ReactiveFormsModule, RouterLink], templateUrl: './rental-return.component.html' })
export class RentalReturnComponent implements OnInit {
  private readonly fb = inject(FormBuilder); private readonly api = inject(DriveEasyApi); private readonly route = inject(ActivatedRoute);
  details: EnrichedRental | null = null; completed: Rental | null = null; loading = true; saving = false; error = '';
  form = this.fb.group({ kmRetour: [0, [Validators.required, Validators.min(0)]] });
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.rental(id).subscribe({ next: x => { this.details = x; this.form.controls.kmRetour.setValue(x.location.kmDepart ?? x.vehicule.kilometrage); this.loading = false; }, error: e => { this.error = e?.error?.error || 'Rental details could not be loaded.'; this.loading = false; } });
  }
  get lateDays() { const due = this.details?.location.dateFinPrevue; if (!due) return 0; const today = new Date(); today.setHours(0, 0, 0, 0); const deadline = new Date(`${due}T00:00:00`); return Math.max(0, Math.floor((today.getTime() - deadline.getTime()) / 86400000)); }
  get penalty() { return (this.details?.location.prixJournalierApplique ?? 0) * 1.5 * this.lateDays; }
  get total() { return (this.details?.location.montantTotal ?? 0) + this.penalty; }
  submit() {
    if (!this.details) return;
    this.form.markAllAsTouched(); if (this.form.invalid) return;
    if (this.form.controls.kmRetour.value! < (this.details.location.kmDepart ?? 0)) { this.error = 'Return mileage must be greater than or equal to departure mileage.'; return; }
    this.error = ''; this.saving = true;
    this.api.returnRental(this.details.location.id, this.form.controls.kmRetour.value!).subscribe({ next: x => { this.completed = x; this.saving = false; }, error: e => { this.error = e?.error?.error || 'Vehicle return could not be completed.'; this.saving = false; } });
  }
  print() { window.print(); }
}
