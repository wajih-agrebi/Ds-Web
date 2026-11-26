package com.driveeasy.rental.dto;

import jakarta.validation.constraints.Min;

public record StartRentalRequest(@Min(0) long kmDepart) { }
