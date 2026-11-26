package com.driveeasy.vehicle.model;

import java.math.BigDecimal;

public record Vehicle(
        Long id,
        String immatriculation,
        String marque,
        String modele,
        VehicleCategory categorie,
        int annee,
        String couleur,
        FuelType carburant,
        Gearbox boite,
        int nbPlaces,
        long kilometrage,
        BigDecimal prixJournalier,
        VehicleStatus statut,
        String imageUrl) { }
