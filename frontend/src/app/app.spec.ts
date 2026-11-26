import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('DriveEasy application shell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [App], providers: [provideRouter(routes)] }).compileComponents();
  });

  it('renders the agency navigation and route outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.brand')?.textContent).toContain('driveeasy');
    expect(root.querySelectorAll('nav a').length).toBe(4);
    expect(root.querySelector('router-outlet')).toBeTruthy();
  });
});
