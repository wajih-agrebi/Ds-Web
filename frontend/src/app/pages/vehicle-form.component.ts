import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DriveEasyApi } from '../core/driveeasy-api.service';
import { VehicleInput } from '../core/models';
import { timeout } from 'rxjs';
import { suggestedVehiclePhoto } from '../core/vehicle-images';

@Component({ standalone: true, imports: [CommonModule, ReactiveFormsModule, RouterLink], templateUrl: './vehicle-form.component.html' })
export class VehicleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(DriveEasyApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  id?: number;
  loading = false;
  saving = false;
  loadedVehicle = false;
  previewImageFailed = false;
  selectedImageFileName = '';
  photoError = '';
  error = '';
  form = this.fb.group({
    immatriculation: ['', Validators.required], marque: ['', Validators.required], modele: ['', Validators.required],
    categorie: ['ECONOMIQUE', Validators.required],
    annee: [new Date().getFullYear(), [Validators.required, Validators.min(1886), Validators.max(new Date().getFullYear())]],
    couleur: ['', Validators.required], carburant: ['ESSENCE', Validators.required], boite: ['MANUELLE', Validators.required],
    nbPlaces: [5, [Validators.required, Validators.min(1)]], kilometrage: [0, [Validators.required, Validators.min(0)]],
    prixJournalier: [100, [Validators.required, Validators.min(0.01)]], statut: ['DISPONIBLE', Validators.required],
    imageUrl: ['', Validators.pattern(/^(?:https?:\/\/\S+|data:image\/(?:jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2})?$/i)],
  });

  get imagePreviewUrl() {
    const customUrl = this.form.controls.imageUrl.value?.trim();
    if (customUrl) return customUrl;
    return suggestedVehiclePhoto({ marque: this.form.controls.marque.value ?? '', modele: this.form.controls.modele.value ?? '' });
  }

  ngOnInit() {
    const rawId = this.route.snapshot.paramMap.get('id');
    if (!rawId) return;
    this.id = Number(rawId);
    this.loadVehicle();
  }

  loadVehicle() {
    if (!this.id) return;
    this.loading = true;
    this.error = '';
    this.api.vehicle(this.id).pipe(timeout({ first: 8000 })).subscribe({
      next: vehicle => {
        this.form.patchValue({ ...vehicle, imageUrl: vehicle.imageUrl || suggestedVehiclePhoto(vehicle) || '' });
        this.loadedVehicle = true;
        this.loading = false;
        this.previewImageFailed = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Loading this vehicle took too long. Check the Vehicle Service and API Gateway, then retry.'
          : error?.status === 0
            ? 'Cannot reach the API Gateway at localhost:8080. Start the backend services, then retry.'
            : error?.error?.error || 'Vehicle could not be loaded.';
        this.loading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  useSuggestedPhoto() {
    const photo = suggestedVehiclePhoto({ marque: this.form.controls.marque.value ?? '', modele: this.form.controls.modele.value ?? '' });
    if (!photo) return;
    this.form.controls.imageUrl.setValue(photo);
    this.form.controls.imageUrl.markAsDirty();
    this.previewImageFailed = false;
    this.selectedImageFileName = '';
    this.photoError = '';
  }

  onImageFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.photoError = 'Choose a JPG, PNG, or WebP image.';
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.photoError = 'Choose an image smaller than 2 MB.';
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result !== 'string') {
        this.photoError = 'This image could not be read. Try another file.';
        this.changeDetector.markForCheck();
        return;
      }
      this.form.controls.imageUrl.setValue(reader.result);
      this.form.controls.imageUrl.markAsDirty();
      this.selectedImageFileName = file.name;
      this.photoError = '';
      this.previewImageFailed = false;
      this.changeDetector.markForCheck();
    };
    reader.onerror = () => {
      this.photoError = 'This image could not be read. Try another file.';
      this.changeDetector.markForCheck();
    };
    reader.readAsDataURL(file);
  }

  clearPhoto() {
    this.form.controls.imageUrl.setValue('');
    this.form.controls.imageUrl.markAsDirty();
    this.selectedImageFileName = '';
    this.photoError = '';
    this.previewImageFailed = false;
  }

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();
    const vehicle = { ...value, imageUrl: value.imageUrl?.trim() || null } as VehicleInput;
    this.api.saveVehicle(vehicle, this.id).pipe(timeout({ first: 10000 })).subscribe({
      next: () => this.router.navigateByUrl('/vehicules'),
      error: error => {
        this.error = error?.name === 'TimeoutError'
          ? 'Saving took too long. Check the vehicle list before trying again.'
          : error?.error?.error || 'Vehicle could not be saved.';
        this.saving = false;
        this.changeDetector.markForCheck();
      },
    });
  }
}
