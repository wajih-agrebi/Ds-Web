package com.driveeasy.vehicle.dto;

import jakarta.validation.constraints.Min;

public record VehicleReturnRequest(@Min(0) long kilometrage) { }
