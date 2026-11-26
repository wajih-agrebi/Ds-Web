package com.driveeasy.rental.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRentalRequest(@Positive long clientId, @Positive long vehiculeId,
                                  @NotNull LocalDate dateDebut, @NotNull LocalDate dateFinPrevue) { }
