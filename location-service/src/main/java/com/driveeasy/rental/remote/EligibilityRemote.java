package com.driveeasy.rental.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EligibilityRemote(long clientId, boolean eligible, int age, boolean permisValide,
                                String categorieClient, String message) { }
