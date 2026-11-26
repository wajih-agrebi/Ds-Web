package com.driveeasy.rental.dto;

import com.driveeasy.rental.model.Rental;

public record EnrichedRental(Rental location, Object client, Object vehicule) { }
