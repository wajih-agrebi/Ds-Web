package com.driveeasy.rental.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.driveeasy.rental.model.Rental;

@Repository
public class InMemoryRentalRepository implements RentalRepository {
    private final ConcurrentHashMap<Long, Rental> rentals = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    @Override public List<Rental> findAll() { return rentals.values().stream().sorted(Comparator.comparing(Rental::id)).toList(); }
    @Override public Optional<Rental> findById(long id) { return Optional.ofNullable(rentals.get(id)); }
    @Override public Rental save(Rental rental) {
        long id = rental.id() == null ? ids.incrementAndGet() : rental.id();
        ids.accumulateAndGet(id, Math::max);
        Rental stored = new Rental(id, rental.clientId(), rental.vehiculeId(), rental.dateDebut(), rental.dateFinPrevue(), rental.dateRetourEffectif(), rental.kmDepart(), rental.kmRetour(), rental.prixJournalierApplique(), rental.montantTotal(), rental.penalite(), rental.statut(), rental.dateCreation(), rental.vehicleReservationId());
        rentals.put(id, stored);
        return stored;
    }
}
