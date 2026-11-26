package com.driveeasy.vehicle.dto;

import jakarta.validation.constraints.Min;

public record MileageRequest(@Min(0) long kilometrage) { }
