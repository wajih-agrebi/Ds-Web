import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DriveEasyApi } from './driveeasy-api.service';

describe('DriveEasyApi', () => {
  let api: DriveEasyApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [DriveEasyApi, provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(DriveEasyApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends browser calls to the API Gateway vehicle route', () => {
    api.vehicles({ statut: 'DISPONIBLE' }).subscribe();
    const request = http.expectOne(r => r.url === 'http://localhost:8080/api/vehicules');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('statut')).toBe('DISPONIBLE');
    request.flush([]);
  });
});
