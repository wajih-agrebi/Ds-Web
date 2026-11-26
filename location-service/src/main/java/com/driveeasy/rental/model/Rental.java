package com.driveeasy.rental.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Rental(Long id, long clientId, long vehiculeId, LocalDate dateDebut, LocalDate dateFinPrevue,
                     LocalDate dateRetourEffectif, Long kmDepart, Long kmRetour, BigDecimal prixJournalierApplique,
                     BigDecimal montantTotal, BigDecimal penalite, RentalStatus statut, LocalDate dateCreation,
                     String vehicleReservationId) { }
