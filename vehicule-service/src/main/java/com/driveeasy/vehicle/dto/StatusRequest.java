package com.driveeasy.vehicle.dto;

import com.driveeasy.vehicle.model.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull VehicleStatus statut) { }
