package com.driveeasy.rental.remote;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleRemote(long id, String immatriculation, String marque, String modele, String categorie,
                            BigDecimal prixJournalier, String statut, long kilometrage) { }
