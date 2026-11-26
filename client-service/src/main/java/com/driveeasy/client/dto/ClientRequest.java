package com.driveeasy.client.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotBlank String cin,
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotNull LocalDate dateNaissance,
        @NotBlank @Email String email,
        @NotBlank String telephone,
        @NotBlank String adresse,
        @NotBlank String numeroPermis,
        @NotNull LocalDate dateExpirationPermis) { }
