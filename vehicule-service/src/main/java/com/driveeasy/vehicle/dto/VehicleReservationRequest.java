package com.driveeasy.vehicle.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record VehicleReservationRequest(@NotNull LocalDate dateDebut, @NotNull LocalDate dateFin) { }
