package com.driveeasy.vehicle.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.driveeasy.vehicle.dto.VehicleRequest;
import com.driveeasy.vehicle.exception.ApiException;
import com.driveeasy.vehicle.model.FuelType;
import com.driveeasy.vehicle.model.Vehicle;
import com.driveeasy.vehicle.model.VehicleStatus;
import com.driveeasy.vehicle.model.VehicleCategory;
import com.driveeasy.vehicle.repository.VehicleRepository;

@Service
public class VehicleService {
    private record HeldPeriod(String reservationId, LocalDate start, LocalDate end) { }

    private final VehicleRepository repository;
    private final Map<Long, Map<String, HeldPeriod>> reservations = new ConcurrentHashMap<>();

    public VehicleService(VehicleRepository repository) { this.repository = repository; }

    public List<Vehicle> find(String categorie, String carburant, String statut, BigDecimal prixMax, LocalDate dateDebut, LocalDate dateFin) {
        validatePeriod(dateDebut, dateFin);
        return repository.findAll().stream()
                .filter(v -> categorie == null || v.categorie() == parseCategory(categorie))
                .filter(v -> carburant == null || v.carburant() == parseFuel(carburant))
                .filter(v -> statut == null || v.statut() == parseStatus(statut))
                .filter(v -> prixMax == null || v.prixJournalier().compareTo(prixMax) <= 0)
                .filter(v -> dateDebut == null || isAvailable(v.id(), dateDebut, dateFin) && v.statut() == VehicleStatus.DISPONIBLE)
                .toList();
    }

    public Vehicle get(long id) { return repository.findById(id).orElseThrow(() -> notFound(id)); }

    public synchronized Vehicle create(VehicleRequest request) {
        validateYear(request.annee());
        ensureUniqueRegistration(request.immatriculation(), null);
        return repository.save(toVehicle(null, request));
    }

    public synchronized Vehicle update(long id, VehicleRequest request) {
        Vehicle existing = get(id);
        validateYear(request.annee());
        ensureUniqueRegistration(request.immatriculation(), id);
        return repository.save(toVehicle(existing.id(), request));
    }

    public synchronized void delete(long id) {
        Vehicle vehicle = get(id);
        if (vehicle.statut() == VehicleStatus.LOUE || hasReservations(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "A rented or reserved vehicle cannot be deleted");
        }
        repository.deleteById(id);
    }

    public synchronized Vehicle updateStatus(long id, VehicleStatus status) {
        Vehicle vehicle = get(id);
        if (status == VehicleStatus.MAINTENANCE && hasReservations(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "A vehicle with active reservations cannot enter maintenance");
        }
        return repository.save(new Vehicle(vehicle.id(), vehicle.immatriculation(), vehicle.marque(), vehicle.modele(), vehicle.categorie(), vehicle.annee(), vehicle.couleur(), vehicle.carburant(), vehicle.boite(), vehicle.nbPlaces(), vehicle.kilometrage(), vehicle.prixJournalier(), status, vehicle.imageUrl()));
    }

    public synchronized Vehicle returnVehicle(long id, long kilometrage) {
        Vehicle vehicle = get(id);
        if (vehicle.statut() != VehicleStatus.LOUE) throw new ApiException(HttpStatus.CONFLICT, "Only a rented vehicle can be returned");
        if (kilometrage < vehicle.kilometrage()) throw new ApiException(HttpStatus.BAD_REQUEST, "Return mileage cannot be lower than recorded mileage");
        return repository.save(new Vehicle(vehicle.id(), vehicle.immatriculation(), vehicle.marque(), vehicle.modele(), vehicle.categorie(), vehicle.annee(), vehicle.couleur(), vehicle.carburant(), vehicle.boite(), vehicle.nbPlaces(), kilometrage, vehicle.prixJournalier(), VehicleStatus.DISPONIBLE, vehicle.imageUrl()));
    }

    public synchronized Vehicle updateMileage(long id, long kilometrage) {
        Vehicle vehicle = get(id);
        if (kilometrage < vehicle.kilometrage()) throw new ApiException(HttpStatus.BAD_REQUEST, "Mileage cannot decrease");
        return repository.save(new Vehicle(vehicle.id(), vehicle.immatriculation(), vehicle.marque(), vehicle.modele(), vehicle.categorie(), vehicle.annee(), vehicle.couleur(), vehicle.carburant(), vehicle.boite(), vehicle.nbPlaces(), kilometrage, vehicle.prixJournalier(), vehicle.statut(), vehicle.imageUrl()));
    }

    /** Reserves a date range atomically so two rentals cannot claim the same vehicle. */
    public synchronized String hold(long id, LocalDate start, LocalDate end) {
        validatePeriod(start, end);
        Vehicle vehicle = get(id);
        if (vehicle.statut() != VehicleStatus.DISPONIBLE || !isAvailable(id, start, end)) {
            throw new ApiException(HttpStatus.CONFLICT, "Vehicle is not available for the requested period");
        }
        String reservationId = UUID.randomUUID().toString();
        reservations.computeIfAbsent(id, ignored -> new ConcurrentHashMap<>())
                .put(reservationId, new HeldPeriod(reservationId, start, end));
        return reservationId;
    }

    public synchronized void release(long id, String reservationId) {
        get(id);
        Map<String, HeldPeriod> periods = reservations.get(id);
        if (periods != null) {
            periods.remove(reservationId);
            if (periods.isEmpty()) reservations.remove(id);
        }
    }

    private boolean isAvailable(long id, LocalDate start, LocalDate end) {
        if (start == null) return true;
        return reservations.getOrDefault(id, Map.of()).values().stream()
                .noneMatch(p -> !start.isAfter(p.end()) && !end.isBefore(p.start()));
    }

    private boolean hasReservations(long id) { return !reservations.getOrDefault(id, Map.of()).isEmpty(); }
    private void ensureUniqueRegistration(String value, Long thisId) {
        boolean duplicate = repository.findAll().stream().anyMatch(v -> v.immatriculation().equalsIgnoreCase(value.trim()) && !v.id().equals(thisId));
        if (duplicate) throw new ApiException(HttpStatus.CONFLICT, "Registration number already exists");
    }
    private void validateYear(int year) {
        if (year > Year.now().getValue()) throw new ApiException(HttpStatus.BAD_REQUEST, "Vehicle year cannot be in the future");
    }
    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null && end == null) return;
        if (start == null || end == null || end.isBefore(start) || start.isBefore(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide a valid date range that starts today or later");
    }
    private FuelType parseFuel(String value) {
        try { return FuelType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown fuel type"); }
    }
    private VehicleStatus parseStatus(String value) {
        try { return VehicleStatus.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown vehicle status"); }
    }
    private VehicleCategory parseCategory(String value) {
        try { return VehicleCategory.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown vehicle category"); }
    }
    private Vehicle toVehicle(Long id, VehicleRequest r) {
        return new Vehicle(id, r.immatriculation().trim().toUpperCase(), r.marque().trim(), r.modele().trim(), r.categorie() == null ? VehicleCategory.AUTRE : r.categorie(), r.annee(), r.couleur().trim(), r.carburant(), r.boite(), r.nbPlaces(), r.kilometrage(), r.prixJournalier(), r.statut(), r.imageUrl());
    }
    private ApiException notFound(long id) { return new ApiException(HttpStatus.NOT_FOUND, "Vehicle " + id + " was not found"); }
}
