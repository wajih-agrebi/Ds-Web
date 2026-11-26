package com.driveeasy.rental.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.driveeasy.rental.dto.CreateRentalRequest;
import com.driveeasy.rental.dto.EnrichedRental;
import com.driveeasy.rental.dto.PageResponse;
import com.driveeasy.rental.dto.PriceQuote;
import com.driveeasy.rental.dto.RentalStatistics;
import com.driveeasy.rental.exception.ApiException;
import com.driveeasy.rental.model.Rental;
import com.driveeasy.rental.model.RentalStatus;
import com.driveeasy.rental.remote.EligibilityRemote;
import com.driveeasy.rental.remote.RentalRemoteClients;
import com.driveeasy.rental.remote.VehicleRemote;
import com.driveeasy.rental.repository.RentalRepository;

@Service
public class RentalService {
    private static final Logger log = LoggerFactory.getLogger(RentalService.class);
    private static final BigDecimal STANDARD_COEFFICIENT = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final RentalRepository repository;
    private final RentalRemoteClients remotes;

    public RentalService(RentalRepository repository, RentalRemoteClients remotes) {
        this.repository = repository;
        this.remotes = remotes;
    }

    public PriceQuote simulate(CreateRentalRequest request) {
        validateDates(request.dateDebut(), request.dateFinPrevue());
        EligibilityRemote client = remotes.eligibility(request.clientId());
        if (!client.eligible()) throw new ApiException(HttpStatus.BAD_REQUEST, client.message());
        VehicleRemote vehicle = remotes.vehicle(request.vehiculeId());
        ensureAvailable(request.vehiculeId(), request.dateDebut(), request.dateFinPrevue());
        return price(vehicle.prixJournalier(), request.dateDebut(), request.dateFinPrevue(), client.categorieClient());
    }

    public synchronized Rental create(CreateRentalRequest request) {
        PriceQuote quote = simulate(request);
        String holdId = remotes.holdVehicle(request.vehiculeId(), request.dateDebut(), request.dateFinPrevue());
        Rental reservation = repository.save(new Rental(null, request.clientId(), request.vehiculeId(), request.dateDebut(), request.dateFinPrevue(), null,
                null, null, quote.prixJournalier(), quote.montantEstime(), ZERO, RentalStatus.RESERVEE, LocalDate.now(), holdId));
        try {
            remotes.clientRentalActive(request.clientId(), reservation.id(), true);
            return reservation;
        } catch (RuntimeException failure) {
            repository.save(copy(reservation, RentalStatus.ANNULEE, reservation.dateRetourEffectif(), reservation.kmDepart(), reservation.kmRetour(), reservation.montantTotal(), reservation.penalite()));
            bestEffort(() -> remotes.clientRentalActive(request.clientId(), reservation.id(), false), "clear client activity marker after failed reservation");
            bestEffort(() -> remotes.releaseVehicle(reservation.vehiculeId(), holdId), "release vehicle hold after failed client activity update");
            throw failure;
        }
    }

    public EnrichedRental get(long id) {
        Rental rental = find(id);
        return new EnrichedRental(rental, remotes.clientDetails(rental.clientId()), remotes.vehicle(rental.vehiculeId()));
    }

    public PageResponse<Rental> search(int page, int size, RentalStatus status, Long clientId, Long vehicleId,
                                       LocalDate dateDebut, LocalDate dateFin) {
        if (page < 0 || size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be >= 0 and size must be from 1 to 100");
        List<Rental> filtered = repository.findAll().stream()
                .filter(r -> status == null || r.statut() == status)
                .filter(r -> clientId == null || r.clientId() == clientId)
                .filter(r -> vehicleId == null || r.vehiculeId() == vehicleId)
                .filter(r -> dateDebut == null || !r.dateFinPrevue().isBefore(dateDebut))
                .filter(r -> dateFin == null || !r.dateDebut().isAfter(dateFin))
                .toList();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageResponse<>(filtered.subList(from, to), page, size, filtered.size(), (int) Math.ceil((double) filtered.size() / size));
    }

    public synchronized Rental start(long id, long mileage) {
        Rental rental = find(id);
        requireStatus(rental, RentalStatus.RESERVEE, "Only a reservation can be started");
        if (LocalDate.now().isBefore(rental.dateDebut())) throw new ApiException(HttpStatus.CONFLICT, "A rental cannot start before its reserved start date");
        VehicleRemote vehicle = remotes.vehicle(rental.vehiculeId());
        if (mileage < vehicle.kilometrage()) throw new ApiException(HttpStatus.BAD_REQUEST, "Departure mileage cannot be below the current vehicle mileage");
        remotes.vehicleStatus(rental.vehiculeId(), "LOUE");
        try {
            remotes.vehicleMileage(rental.vehiculeId(), mileage);
            return repository.save(copy(rental, RentalStatus.EN_COURS, null, mileage, null, rental.montantTotal(), ZERO));
        } catch (RuntimeException failure) {
            bestEffort(() -> remotes.vehicleStatus(rental.vehiculeId(), "DISPONIBLE"), "restore vehicle status after failed rental start");
            throw failure;
        }
    }

    public synchronized Rental returnVehicle(long id, long mileage) {
        Rental rental = find(id);
        requireStatus(rental, RentalStatus.EN_COURS, "Only an active rental can be returned");
        if (rental.kmDepart() == null || mileage < rental.kmDepart()) throw new ApiException(HttpStatus.BAD_REQUEST, "Return mileage cannot be below departure mileage");
        LocalDate today = LocalDate.now();
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(rental.dateFinPrevue(), today));
        BigDecimal penalty = rental.prixJournalierApplique().multiply(new BigDecimal("1.5"))
                .multiply(BigDecimal.valueOf(lateDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = rental.montantTotal().add(penalty).setScale(2, RoundingMode.HALF_UP);
        remotes.returnVehicle(rental.vehiculeId(), mileage);
        Rental completed = repository.save(copy(rental, RentalStatus.TERMINEE, today, rental.kmDepart(), mileage, total, penalty));
        bestEffort(() -> remotes.releaseVehicle(rental.vehiculeId(), rental.vehicleReservationId()), "release completed vehicle reservation hold");
        bestEffort(() -> remotes.clientRentalActive(rental.clientId(), rental.id(), false), "clear client activity marker after completed rental");
        long completedCount = repository.findAll().stream().filter(r -> r.clientId() == rental.clientId() && r.statut() == RentalStatus.TERMINEE).count();
        String earned = completedCount >= 15 ? "VIP" : completedCount >= 5 ? "FIDELE" : null;
        if (earned != null) bestEffort(() -> remotes.updateCategory(rental.clientId(), earned), "update client loyalty category");
        return completed;
    }

    public synchronized Rental cancel(long id) {
        Rental rental = find(id);
        requireStatus(rental, RentalStatus.RESERVEE, "Only a reservation can be cancelled");
        remotes.releaseVehicle(rental.vehiculeId(), rental.vehicleReservationId());
        try {
            remotes.clientRentalActive(rental.clientId(), rental.id(), false);
        } catch (RuntimeException failure) {
            bestEffort(() -> remotes.clientRentalActive(rental.clientId(), rental.id(), true), "restore client activity marker after failed cancellation");
            try {
                String replacementHold = remotes.holdVehicle(rental.vehiculeId(), rental.dateDebut(), rental.dateFinPrevue());
                repository.save(new Rental(rental.id(), rental.clientId(), rental.vehiculeId(), rental.dateDebut(), rental.dateFinPrevue(), rental.dateRetourEffectif(), rental.kmDepart(), rental.kmRetour(), rental.prixJournalierApplique(), rental.montantTotal(), rental.penalite(), rental.statut(), rental.dateCreation(), replacementHold));
            } catch (RuntimeException compensationFailure) { log.error("Could not restore vehicle hold after failed cancellation", compensationFailure); }
            throw failure;
        }
        return repository.save(copy(rental, RentalStatus.ANNULEE, null, null, null, rental.montantTotal(), ZERO));
    }

    public RentalStatistics statistics() {
        List<Rental> rentals = repository.findAll();
        BigDecimal allRevenue = rentals.stream().filter(r -> r.statut() == RentalStatus.TERMINEE)
                .map(Rental::montantTotal).reduce(ZERO, BigDecimal::add);
        YearMonth month = YearMonth.now();
        BigDecimal monthRevenue = rentals.stream().filter(r -> r.statut() == RentalStatus.TERMINEE && YearMonth.from(r.dateRetourEffectif()).equals(month))
                .map(Rental::montantTotal).reduce(ZERO, BigDecimal::add);
        List<VehicleRemote> fleet = remotes.fleet();
        double occupancy = fleet.isEmpty() ? 0 : 100.0 * fleet.stream().filter(v -> "LOUE".equals(v.statut())).count() / fleet.size();
        List<RentalStatistics.PopularVehicle> mostRented = rentals.stream().filter(r -> r.statut() != RentalStatus.ANNULEE)
                .collect(java.util.stream.Collectors.groupingBy(Rental::vehiculeId, java.util.stream.Collectors.counting()))
                .entrySet().stream().map(e -> new RentalStatistics.PopularVehicle(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(RentalStatistics.PopularVehicle::nombreLocations).reversed()).limit(5).toList();
        return new RentalStatistics(allRevenue, monthRevenue, Math.round(occupancy * 100.0) / 100.0, mostRented);
    }

    private PriceQuote price(BigDecimal daily, LocalDate start, LocalDate end, String category) {
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end));
        BigDecimal base = daily.multiply(BigDecimal.valueOf(days)).multiply(STANDARD_COEFFICIENT);
        BigDecimal discountRate = "VIP".equals(category) ? new BigDecimal("0.10") : "FIDELE".equals(category) ? new BigDecimal("0.05") : ZERO;
        BigDecimal discount = base.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = base.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        return new PriceQuote(days, daily, STANDARD_COEFFICIENT, discount, amount, category);
    }
    private void ensureAvailable(long vehicleId, LocalDate start, LocalDate end) {
        boolean available = remotes.availableVehicles(start, end).stream().anyMatch(v -> v.id() == vehicleId);
        if (!available) throw new ApiException(HttpStatus.CONFLICT, "Vehicle is not available for the requested period");
    }
    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start) || start.isBefore(LocalDate.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "Rental dates must be valid and start today or later");
    }
    private Rental find(long id) { return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental " + id + " was not found")); }
    private void requireStatus(Rental rental, RentalStatus expected, String message) { if (rental.statut() != expected) throw new ApiException(HttpStatus.CONFLICT, message); }
    private Rental copy(Rental r, RentalStatus status, LocalDate returned, Long kmStart, Long kmEnd, BigDecimal total, BigDecimal penalty) {
        return new Rental(r.id(), r.clientId(), r.vehiculeId(), r.dateDebut(), r.dateFinPrevue(), returned, kmStart, kmEnd, r.prixJournalierApplique(), total, penalty, status, r.dateCreation(), r.vehicleReservationId());
    }
    private void bestEffort(Runnable action, String description) {
        try { action.run(); } catch (RuntimeException ex) { log.error("Compensation failed while trying to {}", description, ex); }
    }
}
