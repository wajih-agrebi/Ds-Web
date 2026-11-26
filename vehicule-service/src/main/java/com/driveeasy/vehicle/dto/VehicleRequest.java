package com.driveeasy.vehicle.dto;

import java.math.BigDecimal;

import com.driveeasy.vehicle.model.FuelType;
import com.driveeasy.vehicle.model.Gearbox;
import com.driveeasy.vehicle.model.VehicleStatus;
import com.driveeasy.vehicle.model.VehicleCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
        @NotBlank String immatriculation,
        @NotBlank String marque,
        @NotBlank String modele,
        VehicleCategory categorie,
        @Min(1886) int annee,
        @NotBlank String couleur,
        @NotNull FuelType carburant,
        @NotNull Gearbox boite,
        @Min(1) int nbPlaces,
        @Min(0) long kilometrage,
        @NotNull @DecimalMin(value = "0.01") BigDecimal prixJournalier,
        @NotNull VehicleStatus statut,
        String imageUrl) { }
