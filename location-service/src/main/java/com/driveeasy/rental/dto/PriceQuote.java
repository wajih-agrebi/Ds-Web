package com.driveeasy.rental.dto;

import java.math.BigDecimal;

public record PriceQuote(long nombreJours, BigDecimal prixJournalier, BigDecimal coefficientCategorie,
                         BigDecimal remiseFidelite, BigDecimal montantEstime, String categorieClient) { }
