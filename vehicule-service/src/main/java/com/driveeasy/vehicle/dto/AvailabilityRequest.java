package com.driveeasy.vehicle.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(@NotNull LocalDate dateDebut, @NotNull LocalDate dateFin) { }
