package com.driveeasy.client.model;

import java.time.LocalDate;

public record Client(
        Long id,
        String cin,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        String email,
        String telephone,
        String adresse,
        String numeroPermis,
        LocalDate dateExpirationPermis,
        CustomerCategory categorieClient,
        LocalDate dateInscription) { }
