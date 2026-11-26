package com.driveeasy.rental.dto;

import jakarta.validation.constraints.Min;

public record ReturnRentalRequest(@Min(0) long kmRetour) { }
